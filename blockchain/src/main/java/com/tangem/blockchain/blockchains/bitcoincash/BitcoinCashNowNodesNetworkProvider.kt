package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

@Suppress("MagicNumber")
class BitcoinCashNowNodesNetworkProvider(
    val credentials: Pair<String, String>,
    val bchBookUrl: String,
    val bchUrl: String,
) : BitcoinNetworkProvider {

    override val baseUrl: String = bchBookUrl

    private val api: BitcoinCashNowNodesApiService = BitcoinCashNowNodesApiService(bchBookUrl, bchUrl, credentials)

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val getAddressResponse = withContext(Dispatchers.IO) { api.getAddress(address) }
            val getUtxoResponseItems = withContext(Dispatchers.IO) { api.getUtxo(address) }
            val balance = getAddressResponse.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO

            Result.Success(
                BitcoinAddressInfo(
                    balance = balance.movePointLeft(Blockchain.BitcoinCash.decimals()),
                    unspentOutputs = createUnspentOutputs(
                        getUtxoResponseItems = getUtxoResponseItems,
                        transactions = getAddressResponse.transactions.orEmpty(),
                        address = address,
                    ),
                    recentTransactions = createRecentTransactions(
                        transactions = getAddressResponse.transactions.orEmpty(),
                        address = address,
                    ),
                    hasUnconfirmed = getAddressResponse.unconfirmedTxs != 0,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val getFeeResponse = withContext(Dispatchers.IO) { api.getFee() }

            val result = getFeeResponse.result

            if (result <= 0) throw BlockchainSdkError.FailedToLoadFee

            val fee = result
                .toBigDecimal()
                .setScale(Blockchain.BitcoinCash.decimals(), RoundingMode.UP)

            Result.Success(BitcoinFee(fee, fee, fee))
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = withContext(Dispatchers.IO) { api.sendTransaction(transaction) }

            val result = response.result as String

            if (result.isNotBlank()) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.FailedToSendException)
            }
        } catch (e: Exception) {
            SimpleResult.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val response = withContext(Dispatchers.IO) { api.getAddress(address) }
            Result.Success(response.txs.plus(response.unconfirmedTxs ?: 0))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun createUnspentOutputs(
        getUtxoResponseItems: List<GetUtxoResponseItem>,
        transactions: List<GetAddressResponse.Transaction>,
        address: String,
    ): List<BitcoinUnspentOutput> {
        val outputScript = transactions.firstNotNullOfOrNull { transaction ->
            transaction.vout.firstOrNull { it.addresses?.contains(address) == true }?.hex
        } ?: return emptyList()

        return getUtxoResponseItems.mapNotNull {
            val amount = it.value.toBigDecimalOrNull()?.movePointLeft(Blockchain.BitcoinCash.decimals())
                ?: return@mapNotNull null

            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = it.vout.toLong(),
                transactionHash = it.txid.hexToBytes(),
                outputScript = outputScript.hexToBytes(),
            )
        }
    }

    private fun createRecentTransactions(
        transactions: List<GetAddressResponse.Transaction>,
        address: String,
    ): List<BasicTransactionData> {
        return transactions
            .filter { it.confirmations == 0 }
            .map { transaction ->
                val isIncoming = transaction.vin.any { it.addresses?.contains(address) == false }
                var source = "unknown"
                var destination = "unknown"
                val amount = if (isIncoming) {
                    destination = address
                    transaction.vin
                        .firstOrNull()
                        ?.addresses
                        ?.firstOrNull()
                        ?.let { source = it }
                    val outputs = transaction.vout
                        .find { it.addresses?.contains(address) == true }
                        ?.value.toBigDecimalOrDefault()
                    val inputs = transaction.vin
                        .find { it.addresses?.contains(address) == true }
                        ?.value.toBigDecimalOrDefault()
                    outputs - inputs
                } else {
                    source = address
                    transaction.vout
                        .firstOrNull()
                        ?.addresses
                        ?.firstOrNull()
                        ?.let { destination = it }
                    val outputs = transaction.vout
                        .asSequence()
                        .filter { it.addresses?.contains(address) == false }
                        .mapNotNull { it.value?.toBigDecimalOrNull() }
                        .sumOf { it }
                    val fee = transaction.fees.toBigDecimalOrDefault()
                    outputs + fee
                }.movePointLeft(Blockchain.BitcoinCash.decimals())

                BasicTransactionData(
                    balanceDif = if (isIncoming) amount else amount.negate(),
                    hash = transaction.txid,
                    date = Calendar.getInstance().apply {
                        timeInMillis = transaction.blockTime.toLong()
                    },
                    isConfirmed = false,
                    destination = destination,
                    source = source,
                )
            }
    }
}