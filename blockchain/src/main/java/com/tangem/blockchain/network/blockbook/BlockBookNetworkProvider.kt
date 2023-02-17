package com.tangem.blockchain.network.blockbook

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class BlockBookNetworkProvider(
    val config: BlockBookConfig,
    val blockchain: Blockchain
) : BitcoinNetworkProvider {

    override val host: String = config.host

    private val api: BlockBookApi = BlockBookApi(config, blockchain)

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val getAddressResponse = withContext(Dispatchers.IO) { api.getAddress(address) }
            val getUtxoResponseItems = withContext(Dispatchers.IO) { api.getUtxo(address) }
            val balance = getAddressResponse.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO

            Result.Success(
                BitcoinAddressInfo(
                    balance = balance.movePointLeft(blockchain.decimals()),
                    unspentOutputs = createUnspentOutputs(
                        getUtxoResponseItems = getUtxoResponseItems,
                        transactions = getAddressResponse.transactions.orEmpty(),
                        address = address
                    ),
                    recentTransactions = createRecentTransactions(
                        transactions = getAddressResponse.transactions.orEmpty()
                    ),
                    hasUnconfirmed = getAddressResponse.unconfirmedTxs != 0
                )
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val getFeeResponse = withContext(Dispatchers.IO) { api.getFee() }
            val feeRatePerKb = getFeeResponse.result.feerate
                .toBigDecimal()
                .multiply(BigDecimal(BIT_IN_KB_AMOUNT))
                .movePointLeft(blockchain.decimals())

            val minimalPerKb = BigDecimal(MIN_FEE_COEFFICIENT)
                .times(feeRatePerKb)
                .setScale(blockchain.decimals(), RoundingMode.DOWN)

            val normalPerKb = feeRatePerKb.setScale(blockchain.decimals(), RoundingMode.DOWN)

            val priorityPerKb = BigDecimal(MAX_FEE_COEFFICIENT)
                .times(feeRatePerKb)
                .setScale(blockchain.decimals(), RoundingMode.DOWN)

            Result.Success(BitcoinFee(minimalPerKb, normalPerKb, priorityPerKb))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            withContext(Dispatchers.IO) { api.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (e: Exception) {
            SimpleResult.fromTangemSdkError(e.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val response = withContext(Dispatchers.IO) { api.getAddress(address) }
            Result.Success(response.txs + response.unconfirmedTxs)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun createUnspentOutputs(
        getUtxoResponseItems: List<GetUtxoResponseItem>,
        transactions: List<GetAddressResponse.Transaction>,
        address: String
    ): List<BitcoinUnspentOutput> {
        val outputScript = transactions.firstNotNullOfOrNull { transaction ->
            transaction.vout.firstOrNull { it.addresses.contains(address) }?.hex
        } ?: return emptyList()

        return getUtxoResponseItems.mapNotNull {
            val amount = it.value.toBigDecimalOrNull()?.movePointLeft(blockchain.decimals())
                ?: return@mapNotNull null

            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = it.vout.toLong(),
                transactionHash = it.txid.hexToBytes(),
                outputScript = outputScript.hexToBytes()
            )
        }
    }

    private fun createRecentTransactions(
        transactions: List<GetAddressResponse.Transaction>,
    ): List<BasicTransactionData> {
        return transactions
            .filter { it.confirmations == 0 }
            .mapNotNull {
                BasicTransactionData(
                    balanceDif = it.value.toBigDecimalOrNull()?.movePointLeft(blockchain.decimals())
                        ?: return@mapNotNull null,
                    hash = it.txid,
                    date = Calendar.getInstance().apply {
                        timeInMillis = (it.blockTime * 1000).toLong()
                    },
                    isConfirmed = true
                )
            }
    }

    private companion object {
        const val BIT_IN_KB_AMOUNT = 1024
        const val MIN_FEE_COEFFICIENT = 0.8
        const val MAX_FEE_COEFFICIENT = 1.2
    }
}