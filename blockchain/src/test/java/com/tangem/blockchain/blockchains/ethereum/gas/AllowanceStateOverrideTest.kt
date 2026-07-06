package com.tangem.blockchain.blockchains.ethereum.gas

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainFeatureToggles
import org.junit.Test
import org.kethereum.keccakshortcut.keccak
import java.math.BigInteger

/**
 * Unit tests for the ERC20 allowance state-override helpers.
 *
 * The expected storage keys are derived from the canonical Solidity mapping spec:
 *   - Solidity:  keccak256(pad32(spender) ++ keccak256(pad32(owner) ++ pad32(slot)))
 *   - Vyper:     keccak256(keccak256(pad32(slot) ++ pad32(owner)) ++ pad32(spender))
 *
 * Test values for owner/spender are intentionally non-symmetric so any operand
 * mix-up between SOLIDITY and VYPER layouts is immediately visible.
 */
class AllowanceStateOverrideTest {

    private val owner = "0x1234567890123456789012345678901234567890"
    private val spender = "0x5678901234567890123456789012345678901234"

    // ─── AllowanceKey.compute (Solidity) ────────────────────────────────────

    @Test
    fun `compute solidity slot matches manually derived Solidity formula`() {
        // Independently derive the storage key using the canonical Solidity mapping spec:
        //   inner = keccak256(pad32(owner) ++ pad32(slot))
        //   key   = keccak256(pad32(spender) ++ inner)
        // and confirm AllowanceKey.compute produces the same bytes. This exercises the
        // full pipeline without baking in a brittle hardcoded vector.
        val slot = 1L
        val ownerPadded = pad32Bytes(hexToBytes(owner.removePrefix("0x")))
        val spenderPadded = pad32Bytes(hexToBytes(spender.removePrefix("0x")))
        val slotPadded = pad32Bytes(BigInteger.valueOf(slot).toByteArray())

        val inner = (ownerPadded + slotPadded).keccak()
        val expected = (spenderPadded + inner).keccak()

        val actual = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = slot))

        assertThat(actual.toHex()).isEqualTo(expected.toHex())
    }

    @Test
    fun `compute vyper slot matches manually derived Vyper formula`() {
        // Vyper reverses the operand order in each keccak:
        //   inner = keccak256(pad32(slot) ++ pad32(owner))
        //   key   = keccak256(inner ++ pad32(spender))
        val slot = 1L
        val ownerPadded = pad32Bytes(hexToBytes(owner.removePrefix("0x")))
        val spenderPadded = pad32Bytes(hexToBytes(spender.removePrefix("0x")))
        val slotPadded = pad32Bytes(BigInteger.valueOf(slot).toByteArray())

        val inner = (slotPadded + ownerPadded).keccak()
        val expected = (inner + spenderPadded).keccak()

        val actual = AllowanceKey.compute(owner, spender, AllowanceSlot.vyper(slot = slot))

        assertThat(actual.toHex()).isEqualTo(expected.toHex())
    }

    @Test
    fun `compute solidity slot 2 differs from slot 1`() {
        val keySlot1 = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = 1L))
        val keySlot2 = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = 2L))

        assertThat(keySlot1.toHex()).isNotEqualTo(keySlot2.toHex())
    }

    @Test
    fun `compute is deterministic`() {
        val a = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = 3L))
        val b = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = 3L))

        assertThat(a.toHex()).isEqualTo(b.toHex())
    }

    @Test
    fun `compute is sensitive to owner-spender order`() {
        val ownerFirst = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = 1L))
        val spenderFirst = AllowanceKey.compute(spender, owner, AllowanceSlot.solidity(slot = 1L))

        // Swapping owner and spender must change the storage key — this is the
        // defining property that makes `_allowances[a][b] != _allowances[b][a]`.
        assertThat(ownerFirst.toHex()).isNotEqualTo(spenderFirst.toHex())
    }

    // ─── AllowanceKey.compute (Vyper) ───────────────────────────────────────

    @Test
    fun `vyper layout differs from solidity layout`() {
        val solidityKey = AllowanceKey.compute(owner, spender, AllowanceSlot.solidity(slot = 1L))
        val vyperKey = AllowanceKey.compute(owner, spender, AllowanceSlot.vyper(slot = 1L))

        // Operand order is reversed in Vyper, so the keccak chain produces a different
        // storage key for the same (owner, spender, slot).
        assertThat(solidityKey.toHex()).isNotEqualTo(vyperKey.toHex())
    }

    // ─── ERC-7201 namespace derivation ──────────────────────────────────────

    @Test
    fun `computeErc7201Slot for OZ v5 ERC20 plus 1 matches hardcoded candidate`() {
        // OpenZeppelin v5 ERC20 stores `_allowances` at namespace_base + 1.
        val derived = AllowanceKey.computeErc7201Slot(
            namespace = "openzeppelin.storage.ERC20",
            offsetInStruct = 1L,
        )

        assertThat(derived).isEqualTo(CandidateSlots.OZ_V5_ERC20_ALLOWANCES)
    }

    @Test
    fun `computeErc7201Slot zeros the low byte`() {
        val derived = AllowanceKey.computeErc7201Slot(
            namespace = "openzeppelin.storage.ERC20",
            offsetInStruct = 0L,
        )

        // EIP-7201: namespace_base & ~bytes32(uint256(0xff)) — low byte must be zero.
        val lowByte = derived.and(BigInteger.valueOf(LOW_BYTE_MASK_VALUE))
        assertThat(lowByte).isEqualTo(BigInteger.ZERO)
    }

    // ─── StateOverrideBuilder ───────────────────────────────────────────────

    @Test
    fun `StateOverrideBuilder build wraps stateDiff under lowercased token address`() {
        val token = "0xDAC17F958D2EE523A2206206994597C13D831EC7" // USDT mainnet (mixed case)
        val output = StateOverrideBuilder.build(tokenAddress = token, owner = owner, spender = spender)

        assertThat(output.keys).containsExactly(token.lowercase())
        val tokenObject = output.getValue(token.lowercase())
        assertThat(tokenObject.keys).containsExactly("stateDiff")
    }

    @Test
    fun `StateOverrideBuilder build emits one override per candidate slot`() {
        val output = StateOverrideBuilder.build(
            tokenAddress = "0x0000000000000000000000000000000000000001",
            owner = owner,
            spender = spender,
        )
        val stateDiff = output.values.first().getValue("stateDiff")

        // Each candidate slot must map to a distinct storage key — collisions across
        // candidates would silently shrink the override set and reduce coverage.
        assertThat(stateDiff).hasSize(CandidateSlots.all.size)
    }

    @Test
    fun `StateOverrideBuilder build assigns max uint256 to every key`() {
        val output = StateOverrideBuilder.build(
            tokenAddress = "0x0000000000000000000000000000000000000001",
            owner = owner,
            spender = spender,
        )
        val stateDiff = output.values.first().getValue("stateDiff")

        assertThat(stateDiff.values).containsExactlyElementsIn(List(stateDiff.size) { MAX_UINT256_HEX })
    }

    @Test
    fun `CandidateSlots all includes the OZ v5 ERC-7201 slot`() {
        val containsOzV5 = CandidateSlots.all.any { it.baseSlot == CandidateSlots.OZ_V5_ERC20_ALLOWANCES }
        assertThat(containsOzV5).isTrue()
    }

    @Test
    fun `CandidateSlots all includes Vyper variants`() {
        val vyperCount = CandidateSlots.all.count { it.layout == MappingLayout.VYPER }
        assertThat(vyperCount).isGreaterThan(0)
    }

    // ─── Feature toggle contract ────────────────────────────────────────────

    @Test
    fun `isStateOverrideGasEstimateEnabled defaults to false`() {
        // Backward-compat: existing callers that construct BlockchainFeatureToggles
        // without naming this flag must observe pre-feature behavior (i.e. flag = false).
        val toggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false)
        assertThat(toggles.isStateOverrideGasEstimateEnabled).isFalse()
    }

    @Test
    fun `isStateOverrideGasEstimateEnabled can be turned on explicitly`() {
        val toggles = BlockchainFeatureToggles(
            isYieldSupplyEnabled = false,
            isStateOverrideGasEstimateEnabled = true,
        )
        assertThat(toggles.isStateOverrideGasEstimateEnabled).isTrue()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

    private fun pad32Bytes(input: ByteArray): ByteArray {
        if (input.size >= WORD_SIZE_BYTES) return input.copyOfRange(input.size - WORD_SIZE_BYTES, input.size)
        return ByteArray(WORD_SIZE_BYTES - input.size) + input
    }

    private fun hexToBytes(hex: String): ByteArray {
        val padded = if (hex.length % 2 == 1) "0$hex" else hex
        val bytes = ByteArray(padded.length / 2)
        var i = 0
        while (i < padded.length) {
            bytes[i / 2] = (
                (Character.digit(padded[i], HEX_RADIX) shl HEX_NIBBLE_BITS) +
                    Character.digit(padded[i + 1], HEX_RADIX)
                ).toByte()
            i += 2
        }
        return bytes
    }

    private companion object {
        private const val LOW_BYTE_MASK_VALUE = 0xffL
        private const val WORD_SIZE_BYTES = 32
        private const val HEX_RADIX = 16
        private const val HEX_NIBBLE_BITS = 4
    }
}