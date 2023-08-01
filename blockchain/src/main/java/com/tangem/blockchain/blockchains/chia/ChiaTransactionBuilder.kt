package com.tangem.blockchain.blockchains.chia

import com.tangem.blockchain.blockchains.chia.clvm.Condition
import com.tangem.blockchain.blockchains.chia.clvm.CreateCoinCondition
import com.tangem.blockchain.blockchains.chia.clvm.Program
import com.tangem.blockchain.blockchains.chia.clvm.RemarkCondition
import com.tangem.blockchain.blockchains.chia.extensions.chiaEncode
import com.tangem.blockchain.blockchains.chia.network.ChiaCoin
import com.tangem.blockchain.blockchains.chia.network.ChiaCoinSpend
import com.tangem.blockchain.blockchains.chia.network.ChiaSpendBundle
import com.tangem.blockchain.blockchains.chia.network.ChiaTransactionBody
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blstlib.generated.P2
import com.tangem.blstlib.generated.P2_Affine
import com.tangem.common.extensions.*
import java.math.BigDecimal

class ChiaTransactionBuilder(private val walletPublicKey: ByteArray, val blockchain: Blockchain) {
    private val decimals = blockchain.decimals()
    private val genesisChallenge = when (blockchain) {
        Blockchain.Chia -> "ccd5bb71183532bff220ba46c268991a3ff07eb358e8255a65c30a2dce0e5fbb".hexToBytes()
        Blockchain.ChiaTestnet -> "ae83525ba8d1dd3f09b277de18ca3e43fc0af20d20c4b3e92ef2a48bd291ccb2".hexToBytes()
        else -> throw IllegalStateException("$blockchain isn't supported")
    }

    var unspentCoins: List<ChiaCoin> = emptyList()
    private var coinSpends: List<ChiaCoinSpend> = emptyList()

    fun buildToSign(transactionData: TransactionData): Result<List<ByteArray>> {
        if (unspentCoins.isEmpty()) return Result.Failure(
            BlockchainSdkError.CustomError("Unspent coins are missing")
        )

        val change = calculateChange(transactionData, unspentCoins)

        coinSpends = transactionData.toChiaCoinSpends(unspentCoins, change)

        val hashesForSign = coinSpends.map { it ->
            // our solutions are always Cons
            val conditions = Program.deserialize(it.solution.hexToBytes()) as Program.Cons
            val conditionsHash = conditions.left.hash()

            (conditionsHash + it.coin.calculateId() + genesisChallenge).hashAugScheme()
        }
        return Result.Success(hashesForSign)
    }

    fun buildToSend(signatures: List<ByteArray>) = ChiaTransactionBody(
        ChiaSpendBundle(aggregateSignatures(signatures).toHexString(), coinSpends)
    )

    fun getTransactionCost(amount: Amount): Long {
        val balance = unspentCoins.sumOf { it.amount }
        val change = balance - amount.value!!.toMojo()
        val numberOfCoinsCreated = if (change > 0) 2 else 1

        return (coinSpends.size * COIN_SPEND_COST) + (numberOfCoinsCreated * CREATE_COIN_COST)
    }

    private fun calculateChange(
        transactionData: TransactionData,
        unspentCoins: List<ChiaCoin>,
    ): Long {
        val balance = unspentCoins.sumOf { it.amount }
        return balance -
            (transactionData.amount.value!!.toMojo() + (transactionData.fee?.amount?.value?.toMojo() ?: 0L))
    }

    private fun aggregateSignatures(signatures: List<ByteArray>): ByteArray {
        return try {
            val sum = P2()
            for (signature in signatures) {
                val p2Signature = P2_Affine(signature)
                sum.aggregate(p2Signature)
            }
            sum.compress()
        } catch (e: IllegalArgumentException) {
            // Blst performs a G2 group membership test on each signature. We end up here if it fails.
            throw IllegalStateException("Signature aggregation failed")
        }
    }

    private fun TransactionData.toChiaCoinSpends(
        unspentCoins: List<ChiaCoin>,
        change: Long,
    ): List<ChiaCoinSpend> {
        val coinSpends = unspentCoins.map {
            ChiaCoinSpend(
                coin = it,
                puzzleReveal = ChiaAddressService.getPuzzle(walletPublicKey).toHexString(),
                solution = ""
            )
        }

        val sendCondition = CreateCoinCondition(
            destinationPuzzleHash = ChiaAddressService.getPuzzleHash(this.destinationAddress),
            amount = this.amount.value!!.toMojo()
        )
        val changeCondition = if (change != 0L) {
            CreateCoinCondition(
                destinationPuzzleHash = ChiaAddressService.getPuzzleHash(this.sourceAddress),
                amount = change
            )
        } else {
            null
        }

        coinSpends[0].solution = listOfNotNull(sendCondition, changeCondition).toSolution().toHexString()

        for (coinSpend in coinSpends.drop(1)) {
            coinSpend.solution = listOf(RemarkCondition()).toSolution().toHexString()
        }

        return coinSpends
    }

    private fun List<Condition>.toSolution(): ByteArray {
        val conditions = Program.fromList(this.map { it.toProgram() })
        val solutionArguments = Program.fromList(listOf(conditions)) // might be more than one for other puzzles

        return solutionArguments.serialize()
    }

    private fun ChiaCoin.calculateId() = (
        this.parentCoinInfo.hexToBytes() +
            this.puzzleHash.hexToBytes() +
            this.amount.chiaEncode()
        ).calculateSha256()

    // mojo is the smallest unit of Chia
    private fun BigDecimal.toMojo() = this.movePointRight(decimals).toLong()

    private fun ByteArray.hashAugScheme(): ByteArray {
        return P2().hash_to(walletPublicKey + this, AUG_SCHEME_DST, ByteArray(0)).compress()
    }

    companion object {
        // costs were calculated with get_fee_estimate API method with real transactions of currently use format
        private const val COIN_SPEND_COST = 4500000L
        private const val CREATE_COIN_COST = 2400000L

        private const val AUG_SCHEME_DST = "BLS_SIG_BLS12381G2_XMD:SHA-256_SSWU_RO_AUG_"
    }
}