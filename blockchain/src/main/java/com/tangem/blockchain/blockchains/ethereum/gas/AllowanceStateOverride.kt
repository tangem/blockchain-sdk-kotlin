package com.tangem.blockchain.blockchains.ethereum.gas

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import org.kethereum.keccakshortcut.keccak
import java.math.BigInteger

/**
 * ERC20 allowance gas estimation via JSON-RPC state overrides.
 *
 * Problem: eth_estimateGas on `transferFrom()` reverts when the spender has no allowance,
 * so the SDK can't pre-estimate gas before the user has executed `approve`.
 *
 * Solution: override the `_allowances[owner][spender]` storage slot in the simulation
 * to uint256.max. The EVM thinks the allowance is unlimited and the call succeeds.
 *
 * The challenge: each ERC20 implementation places `_allowances` at a different storage
 * slot. There is no protocol limit on override count, and overriding a slot the token contract
 * doesn't actually use is harmless — the EVM only reads the slot the contract reads.
 *
 * Provider support (confirmed): Infura, Alchemy, NowNodes (default Erigon/Geth-based
 * upstream), self-hosted Geth/Erigon. Providers that don't support state overrides
 * either return "method not found" or silently ignore the third param; the SDK's
 * normal [com.tangem.blockchain.network.MultiNetworkProvider] failover handles the
 * former, and the latter degrades gracefully to the existing "estimate fails on
 * missing allowance" behavior.
 *
 * We rely on the multi-slot candidate list for every token and throw structured
 * error when none of the candidates match, so the team can identify misses without
 * needing to maintain an in-code registry.
 */

// ───────────────────────────────────────────────────────────────────────────
// Constants
// ───────────────────────────────────────────────────────────────────────────
private const val HEX_RADIX = 16
private const val HEX_NIBBLE_BITS = 4
private const val WORD_SIZE_BYTES = 32
private const val LOW_BYTE_MASK_VALUE = 0xff
private const val UINT256_BIT_WIDTH = 256

/** Solidity 0.8 OpenZeppelin v4 upgradeable: `__gap[50]` + `_balances@51` + `_allowances@52`. */
private const val OZ_V4_UPGRADEABLE_ALLOWANCES_SLOT: Long = 52

internal const val MAX_UINT256_HEX = "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

// ───────────────────────────────────────────────────────────────────────────
// Layout descriptor
// ───────────────────────────────────────────────────────────────────────────

/**
 * Storage layout used by a token contract for its `_allowances` mapping.
 *
 * Solidity packs `mapping(K => V)` as `keccak256(pad32(key) ++ pad32(slot))`.
 * Vyper reverses the operand order: `keccak256(pad32(slot) ++ pad32(key))`.
 */
internal enum class MappingLayout {
    SOLIDITY,
    VYPER,
}

/**
 * Describes where `_allowances` lives in a given ERC20 contract.
 *
 * For "flat" layouts, [baseSlot] is a small uint (0, 1, 2, ...).
 * For ERC-7201 namespaced storage (OZ v5 upgradeable), [baseSlot] is the pre-computed
 * namespace slot plus the offset of `_allowances` in the struct.
 */
internal data class AllowanceSlot(
    val baseSlot: BigInteger,
    val layout: MappingLayout = MappingLayout.SOLIDITY,
) {
    companion object {
        fun solidity(slot: Long): AllowanceSlot = AllowanceSlot(BigInteger.valueOf(slot), MappingLayout.SOLIDITY)
        fun vyper(slot: Long): AllowanceSlot = AllowanceSlot(BigInteger.valueOf(slot), MappingLayout.VYPER)
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Candidate slots for the multi-slot fallback (used for every token).
// ───────────────────────────────────────────────────────────────────────────

internal object CandidateSlots {
    /**
     * Pre-computed ERC-7201 base slot for OpenZeppelin v5 ERC20.
     *
     * namespace_base = keccak256(abi.encode(uint256(keccak256("openzeppelin.storage.ERC20")) - 1))
     *                  & ~bytes32(uint256(0xff))
     *                = 0x52c6...ce00
     *
     * Within the `ERC20Storage` struct, `_allowances` is at offset +1, so the
     * concrete slot used here is `namespace_base + 1`. Verify at runtime against
     * [AllowanceKey.computeErc7201Slot] if this value is ever in doubt.
     */
    val OZ_V5_ERC20_ALLOWANCES: BigInteger = BigInteger(
        "52c63247e1f47db19d5ce0460030c497f067ca4cebf71ba98eeadabe20bace01",
        HEX_RADIX,
    )

    /**
     * Realistic `_allowances` slots observed across common ERC20 implementations.
     * Every entry in this list is overridden in a single eth_estimateGas request;
     * overriding a slot the contract doesn't read has no effect.
     */
    @Suppress("MagicNumber")
    val all: List<AllowanceSlot> = buildList {
        // ── Solidity layouts ──────────────────────────────────────────────
        repeat(20) { slotNumber ->
            add(AllowanceSlot.solidity(slot = slotNumber.toLong()))
        }
        // OpenZeppelin ERC20Upgradeable v4:
        // Initializable@0, ContextUpgradeable.__gap[50]@1-50, _balances@51, _allowances@52
        add(AllowanceSlot.solidity(slot = OZ_V4_UPGRADEABLE_ALLOWANCES_SLOT))
        // OpenZeppelin ERC20Upgradeable v5 (ERC-7201 namespaced):
        add(AllowanceSlot(baseSlot = OZ_V5_ERC20_ALLOWANCES, layout = MappingLayout.SOLIDITY))

        // ── Vyper layouts (reversed hash operand order) ───────────────────
        // Vyper ERC20 implementations are rare but exist (notably Curve's tokens).
        repeat(5) { slotNumber ->
            add(AllowanceSlot.vyper(slot = slotNumber.toLong()))
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Storage key computation
// ───────────────────────────────────────────────────────────────────────────

internal object AllowanceKey {

    /**
     * Compute the storage key for `_allowances[owner][spender]`.
     *
     * Solidity nested mapping at base slot S:
     *   inner = keccak256(pad32(owner) ++ pad32(S))
     *   key   = keccak256(pad32(spender) ++ inner)
     *
     * Vyper uses the reverse operand order in each keccak256.
     */
    fun compute(owner: String, spender: String, slot: AllowanceSlot): ByteArray {
        val ownerBytes = pad32(input = hexToBytes(hex = owner))
        val spenderBytes = pad32(input = hexToBytes(hex = spender))
        val slotBytes = pad32(input = slot.baseSlot.toByteArray())

        return when (slot.layout) {
            MappingLayout.SOLIDITY -> {
                val inner = (ownerBytes + slotBytes).toKeccak()
                (spenderBytes + inner).toKeccak()
            }
            MappingLayout.VYPER -> {
                val inner = (slotBytes + ownerBytes).toKeccak()
                (inner + spenderBytes).toKeccak()
            }
        }
    }

    /**
     * Runtime verification helper for an ERC-7201 namespace slot.
     *
     * Spec (EIP-7201):
     *   namespace_base = keccak256(abi.encode(uint256(keccak256(namespace)) - 1)) & ~bytes32(uint256(0xff))
     */
    fun computeErc7201Slot(namespace: String, offsetInStruct: Long = 0L): BigInteger {
        val h1 = namespace.toByteArray(Charsets.UTF_8).keccak()
        val h1MinusOne = BigInteger(1, h1).subtract(BigInteger.ONE)
        val encoded = pad32(input = h1MinusOne.toByteArray())
        val h2 = encoded.keccak()

        // Clear the lowest byte: & ~0xff (within uint256).
        val mask = BigInteger.ONE
            .shiftLeft(UINT256_BIT_WIDTH)
            .subtract(BigInteger.ONE)
            .xor(BigInteger.valueOf(LOW_BYTE_MASK_VALUE.toLong()))
        val base = BigInteger(1, h2).and(mask)

        return base.add(BigInteger.valueOf(offsetInStruct))
    }

    private fun pad32(input: ByteArray): ByteArray {
        // BigInteger.toByteArray() may prepend a sign byte; slicing from the right
        // handles both that case and any already-padded input.
        if (input.size >= WORD_SIZE_BYTES) return input.copyOfRange(input.size - WORD_SIZE_BYTES, input.size)
        return ByteArray(WORD_SIZE_BYTES - input.size) + input
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleaned = hex.removePrefix("0x")
        val padded = if (cleaned.length % 2 == 1) "0$cleaned" else cleaned
        val bytes = ByteArray(padded.length / 2)
        var i = 0
        while (i < padded.length) {
            bytes[i / 2] = ((Character.digit(padded[i], HEX_RADIX) shl HEX_NIBBLE_BITS) +
                Character.digit(padded[i + 1], HEX_RADIX)).toByte()
            i += 2
        }
        return bytes
    }
}

// ───────────────────────────────────────────────────────────────────────────
// State override builder
// ───────────────────────────────────────────────────────────────────────────

internal object StateOverrideBuilder {

    /**
     * Build the `stateDiff` object to attach as the 3rd parameter of
     * `eth_estimateGas` / `eth_call`.
     *
     * Output shape (ready for JSON serialization):
     * ```
     * {
     *   "<token>": {
     *     "stateDiff": {
     *       "<storageKey1>": "0xffff...ff",
     *       "<storageKey2>": "0xffff...ff",
     *       ...
     *     }
     *   }
     * }
     * ```
     *
     * Every candidate slot in [CandidateSlots.all] is overridden — there is no
     * per-token registry. The team logs misses centrally via [AllowanceStateOverrideLogger]
     * so the candidate list can be extended over time.
     */
    fun build(tokenAddress: String, owner: String, spender: String): Map<String, Map<String, Map<String, String>>> {
        val stateDiff: Map<String, String> = CandidateSlots.all.associate { slot ->
            val key = AllowanceKey.compute(owner = owner, spender = spender, slot = slot)
            "0x" + key.toHex() to MAX_UINT256_HEX
        }

        return mapOf(
            tokenAddress.lowercase() to mapOf("stateDiff" to stateDiff),
        )
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
}