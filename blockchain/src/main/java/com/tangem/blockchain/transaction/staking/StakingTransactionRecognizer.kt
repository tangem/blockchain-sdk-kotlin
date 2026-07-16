package com.tangem.blockchain.transaction.staking

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.Array as CborArray
import co.nstant.`in`.cbor.model.Map as CborMap
import co.nstant.`in`.cbor.model.UnsignedInteger
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.transaction.staking.model.EvmStakingTx
import com.tangem.blockchain.transaction.staking.model.TronStakingRawTx
import com.tangem.common.extensions.hexToBytes
import java.math.BigInteger

/**
 * Recognizes whether a raw unsigned transaction is a known staking operation, for networks where
 * staking has a deterministic on-chain signature (Tron, Cosmos, Cardano, Solana, Polygon, BSC).
 *
 * Pure parsing, no I/O. Returns false for any other blockchain or when the payload cannot be parsed
 * (callers treat false as fail-closed).
 */
object StakingTransactionRecognizer {

    private val TRON_STAKING_CONTRACT_TYPES = setOf(
        "FreezeBalanceV2Contract",
        "UnfreezeBalanceV2Contract",
        "CancelAllUnfreezeV2Contract",
        "DelegateResourceContract",
        "UnDelegateResourceContract",
        "WithdrawExpireUnfreezeContract",
        "VoteWitnessContract",
        "WithdrawBalanceContract",
    )
    private val COSMOS_STAKING_TYPE_URL_MARKERS = setOf(
        // Staking module: delegate / undelegate / redelegate / ...
        "/cosmos.staking.",
        // Reward claim is served by the distribution module, not staking. Recognize the specific
        // delegator-reward withdrawal so a legitimate claim is not blocked; keep it message-specific
        // (not the whole "/cosmos.distribution." module) to stay fail-closed.
        "/cosmos.distribution.v1beta1.MsgWithdrawDelegatorReward",
    )
    private val CARDANO_CERTIFICATES_KEY: BigInteger = BigInteger.valueOf(4)
    private val CARDANO_WITHDRAWALS_KEY: BigInteger = BigInteger.valueOf(5)
    private const val SOLANA_STAKE_PROGRAM_HEX = "06a1d8179137542a983437bdfe2a7ab2557f535c8a78722b68a49dc000000000"

    // Trust anchors of the staking signing gate — verify before changing:
    // StakeKit Polygon staking contract.
    private const val POLYGON_STAKEKIT_CONTRACT = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"

    // POL (ex-MATIC) ERC-20 token, approved towards the StakeKit contract before staking.
    private const val POL_TOKEN_CONTRACT = "0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6"

    // BSC StakeHub system precompile (0x..2002).
    private const val BSC_STAKEHUB_CONTRACT = "0x0000000000000000000000000000000000002002"
    private const val ERC20_APPROVE_METHOD_ID = "095ea7b3"
    private const val METHOD_ID_HEX_LENGTH = 8
    private const val WORD_HEX_LENGTH = 64
    private const val ADDRESS_HEX_LENGTH = 40
    private const val APPROVE_DATA_MIN_HEX_LENGTH = METHOD_ID_HEX_LENGTH + WORD_HEX_LENGTH

    private val tronAdapter by lazy {
        Moshi.Builder().build().adapter(TronStakingRawTx::class.java)
    }

    private val evmAdapter by lazy {
        Moshi.Builder().build().adapter(EvmStakingTx::class.java)
    }

    /**
     * @return true if [unsignedTransaction] matches a known staking signature for [blockchain];
     * false if it does not match OR cannot be parsed.
     *
     * A `false` does NOT prove the transaction is non-staking — it may be a chain/format this
     * recognizer does not model. Callers must treat `false` per their own blind-signing policy,
     * not as "safe to sign blindly".
     */
    fun isRecognizedStakingTransaction(blockchain: Blockchain, unsignedTransaction: String): Boolean {
        return runCatching {
            when (blockchain) {
                Blockchain.Tron -> isTronStaking(unsignedTransaction)
                Blockchain.Cosmos -> isCosmosStaking(unsignedTransaction)
                Blockchain.Cardano -> isCardanoStaking(unsignedTransaction)
                Blockchain.Solana -> isSolanaStaking(unsignedTransaction)
                Blockchain.Polygon -> isPolygonStaking(unsignedTransaction)
                Blockchain.BSC -> isEvmStakingTo(unsignedTransaction, BSC_STAKEHUB_CONTRACT)
                // Testnets and all other chains are intentionally not recognized (fail-closed).
                else -> false
            }
        }.getOrDefault(false)
    }

    // Fail-closed: a Tron tx is staking only if it has at least one contract and EVERY contract is a
    // staking operation, so a rogue contract can't ride along with a staking one in a bundled tx.
    private fun isTronStaking(unsignedTransaction: String): Boolean {
        val contracts = tronAdapter.fromJson(unsignedTransaction)?.rawData?.contract
        return !contracts.isNullOrEmpty() && contracts.all { it.type in TRON_STAKING_CONTRACT_TYPES }
    }

    // No Cosmos protobuf model here: detect the staking message by its type URL, which is serialized
    // as plain ASCII. ISO-8859-1 preserves every byte 1:1 so the ASCII marker is findable.
    private fun isCosmosStaking(unsignedTransaction: String): Boolean {
        val decoded = String(unsignedTransaction.hexToBytes(), Charsets.ISO_8859_1)
        return COSMOS_STAKING_TYPE_URL_MARKERS.any { decoded.contains(it) }
    }

    private fun isCardanoStaking(unsignedTransaction: String): Boolean {
        val items = CborDecoder.decode(unsignedTransaction.hexToBytes())
        val body = (items.firstOrNull() as? CborArray)?.dataItems?.firstOrNull() as? CborMap ?: return false
        return body.keys.any { key ->
            key is UnsignedInteger &&
                (key.value == CARDANO_CERTIFICATES_KEY || key.value == CARDANO_WITHDRAWALS_KEY)
        }
    }

    private fun isSolanaStaking(unsignedTransaction: String): Boolean =
        unsignedTransaction.lowercase().contains(SOLANA_STAKE_PROGRAM_HEX)

    private fun isEvmStakingTo(unsignedTransaction: String, contractAddress: String): Boolean {
        val to = evmAdapter.fromJson(unsignedTransaction)?.to ?: return false
        return to.equals(contractAddress, ignoreCase = true)
    }

    // Polygon staking is either a direct call to the StakeKit contract, or an ERC-20 approve of the
    // POL token whose spender is that same StakeKit contract (the pre-stake allowance transaction).
    private fun isPolygonStaking(unsignedTransaction: String): Boolean {
        val tx = evmAdapter.fromJson(unsignedTransaction) ?: return false
        val to = tx.to ?: return false
        return when {
            to.equals(POLYGON_STAKEKIT_CONTRACT, ignoreCase = true) -> true
            to.equals(POL_TOKEN_CONTRACT, ignoreCase = true) -> isApproveTo(tx.data, POLYGON_STAKEKIT_CONTRACT)
            else -> false
        }
    }

    // Checks that calldata is an ERC-20 approve(spender, amount) whose spender equals [spender].
    // Layout: 4-byte methodId | 32-byte spender (left-padded address) | 32-byte amount.
    private fun isApproveTo(data: String?, spender: String): Boolean {
        val hex = (data ?: return false).removePrefix("0x").removePrefix("0X")
        if (hex.length < APPROVE_DATA_MIN_HEX_LENGTH) return false
        if (!hex.startsWith(ERC20_APPROVE_METHOD_ID, ignoreCase = true)) return false
        val spenderWord = hex.substring(METHOD_ID_HEX_LENGTH, METHOD_ID_HEX_LENGTH + WORD_HEX_LENGTH)
        val spenderAddress = "0x" + spenderWord.takeLast(ADDRESS_HEX_LENGTH)
        return spenderAddress.equals(spender, ignoreCase = true)
    }
}