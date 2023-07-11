package com.tangem.blockchain.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem.TransactionDirection
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem.TransactionType
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryState
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

private const val EMPTY_ADDRESS = "empty address"

internal class BitcoinTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val blockBookApi: BlockBookApi,
) : TransactionHistoryProvider {

    override suspend fun getTransactionHistoryState(address: String): TransactionHistoryState {
        return try {
            val addressResponse = withContext(Dispatchers.IO) { blockBookApi.getAddress(address) }
            if (addressResponse.txs > 0) {
                TransactionHistoryState.Success.HasTransactions(addressResponse.txs)
            } else {
                TransactionHistoryState.Success.Empty
            }
        } catch (e: Exception) {
            Log.e(BitcoinTransactionHistoryProvider::class.java.simpleName, e.message, e)
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    override suspend fun getTransactionsHistory(
        address: String,
        page: Int,
        pageSize: Int,
    ): Result<List<TransactionHistoryItem>> {
        return try {
            val transactions =
                withContext(Dispatchers.IO) { blockBookApi.getTransactions(address, page, pageSize) }
            val txs = transactions?.map { tx -> tx.toTransactionHistoryItem(address) }
            Result.Success(txs ?: emptyList())
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItem(walletAddress: String): TransactionHistoryItem {
        val isIncoming = vin?.any { !it.addresses.contains(walletAddress) } ?: false
        return TransactionHistoryItem(
            txHash = txid,
            timestamp = blockTime.toLong(),
            direction = extractTransactionDirection(
                isIncoming = isIncoming,
                tx = this,
                walletAddress = walletAddress
            ),
            status = if (confirmations > 0) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed,
            type = TransactionType.Transfer,
            amount = extractAmount(
                isIncoming = isIncoming,
                tx = this,
                walletAddress = walletAddress,
                blockchain = blockchain,
            )
        )
    }

    private fun extractTransactionDirection(
        isIncoming: Boolean,
        tx: GetAddressResponse.Transaction,
        walletAddress: String,
    ): TransactionDirection {
        val address = if (isIncoming) {
            tx.vin?.find { !it.addresses.contains(walletAddress) }?.addresses?.firstOrNull()
        } else {
            tx.vout?.find { !it.addresses.contains(walletAddress) }?.addresses?.firstOrNull()
        } ?: EMPTY_ADDRESS

        return if (isIncoming) TransactionDirection.Incoming(from = address) else TransactionDirection.Outgoing(to = address)
    }

    private fun extractAmount(
        isIncoming: Boolean,
        tx: GetAddressResponse.Transaction,
        walletAddress: String,
        blockchain: Blockchain,
    ): Amount {
        return try {
            val amount = if (isIncoming) {
                val outputs = tx.vout
                    ?.find { it.addresses.contains(walletAddress) }
                    ?.value.toBigDecimalOrDefault()
                val inputs = tx.vin
                    ?.find { it.addresses.contains(walletAddress) }
                    ?.value.toBigDecimalOrDefault()
                outputs - inputs
            } else {
                val outputs = tx.vout
                    ?.filter { !it.addresses.contains(walletAddress) }
                    ?.mapNotNull { it.value.toBigDecimalOrNull() }
                    ?.sumOf { it }
                    ?: BigDecimal.ZERO
                val fee = tx.fees.toBigDecimalOrDefault()
                outputs + fee
            }

            Amount(value = amount.movePointLeft(blockchain.decimals()), blockchain = blockchain)
        } catch (e: Exception) {
            Amount(blockchain = blockchain)
        }
    }
}
