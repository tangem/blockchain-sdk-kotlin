package com.tangem.blockchain.transactionhistory.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.*
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.common.extensions.guard
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

private const val ETHEREUM_METHOD_ID_LENGTH = 8

internal class EthereumTransactionHistoryItemMapper(private val blockchain: Blockchain) {

    fun convert(
        walletAddress: String,
        filterType: TransactionHistoryRequest.FilterType,
        response: GetAddressResponse,
    ): List<TransactionHistoryItem> {
        val transactions = response.transactions ?: return emptyList()

        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> transactions.mapNotNull { transaction ->
                convertFromCoinTransaction(walletAddress = walletAddress, transaction = transaction)
            }
            is TransactionHistoryRequest.FilterType.Contract -> transactions.flatMap { transaction ->
                convertFromTokenTransaction(
                    walletAddress = walletAddress,
                    token = filterType.tokenInfo,
                    transaction = transaction,
                )
            }
        }
    }

    private fun convertFromCoinTransaction(
        walletAddress: String,
        transaction: GetAddressResponse.Transaction,
    ): TransactionHistoryItem? {
        val source = transaction.extractSourceType() ?: return null
        val destination = transaction.extractDestination() ?: return null

        val isOutgoing = transaction.vin
            .firstOrNull()
            ?.addresses
            ?.firstOrNull()
            .equals(walletAddress, ignoreCase = true)

        return TransactionHistoryItem(
            txHash = transaction.txid,
            timestamp = TimeUnit.SECONDS.toMillis(transaction.blockTime.toLong()),
            isOutgoing = isOutgoing,
            destinationType = destination,
            sourceType = source,
            status = extractStatus(transaction = transaction),
            type = extractType(transaction = transaction),
            amount = Amount(
                value = BigDecimal(transaction.value).movePointLeft(blockchain.decimals()),
                blockchain = blockchain,
                type = AmountType.Coin,
            ),
        )
    }

    private fun convertFromTokenTransaction(
        walletAddress: String,
        token: Token,
        transaction: GetAddressResponse.Transaction,
    ): List<TransactionHistoryItem> {
        return transaction.tokenTransfers
            .asSequence()
            .filter { transfer ->
                // Double check to exclude token transfers sent to self.
                // Actually, this is a feasible case, but we don't support such transfers at the moment
                transfer.from.equalsIgnoreCase(walletAddress) && !transfer.to.equalsIgnoreCase(walletAddress) ||
                    transfer.to.equalsIgnoreCase(walletAddress) && !transfer.from.equalsIgnoreCase(walletAddress)
            }
            .filter { transfer ->
                // Double check to exclude token transfers for different tokens (just in case)
                val contract = transfer.contract ?: return@filter false
                token.contractAddress.equalsIgnoreCase(contract)
            }
            .mapNotNull { tokenTransfer ->
                val isOutgoing = tokenTransfer.from.equals(walletAddress, ignoreCase = true)
                val amount = tokenTransfer.extractAmount(token)

                if (shouldExcludeTransaction(amount)) {
                    null
                } else {
                    TransactionHistoryItem(
                        txHash = transaction.txid,
                        timestamp = TimeUnit.SECONDS.toMillis(transaction.blockTime.toLong()),
                        isOutgoing = isOutgoing,
                        destinationType = DestinationType.Single(AddressType.User(tokenTransfer.to)),
                        sourceType = SourceType.Single(tokenTransfer.from),
                        status = extractStatus(transaction),
                        type = extractType(transaction),
                        amount = amount,
                    )
                }
            }
            .toList()
    }

    private fun extractStatus(transaction: GetAddressResponse.Transaction): TransactionStatus {
        val status = transaction.ethereumSpecific?.status.guard {
            return if (transaction.confirmations > 0) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed
        }

        return when (status) {
            GetAddressResponse.Transaction.StatusType.PENDING -> TransactionStatus.Unconfirmed
            GetAddressResponse.Transaction.StatusType.FAILURE -> TransactionStatus.Failed
            GetAddressResponse.Transaction.StatusType.OK -> TransactionStatus.Confirmed
        }
    }

    private fun extractType(transaction: GetAddressResponse.Transaction): TransactionType {
        val ethereumSpecific = transaction.ethereumSpecific

        // Retrieve the methodId from a specific field in the response or parse it from ethereumSpecific. If unable to
        // extract the methodId from either, return the default transaction type.
        val methodId = ethereumSpecific?.parsedData?.methodId
            ?: methodIdFromRawData(ethereumSpecific?.data)
            ?: return TransactionType.Transfer

        // MethodId is empty for the coin transfers
        if (methodId.isEmpty()) return TransactionType.Transfer

        return TransactionType.ContractMethod(id = methodId, callData = ethereumSpecific?.data)
    }

    private fun methodIdFromRawData(rawData: String?): String? {
        val methodId = rawData?.removePrefix("0x")?.take(ETHEREUM_METHOD_ID_LENGTH)
        return if (methodId?.length == ETHEREUM_METHOD_ID_LENGTH) methodId else null
    }

    private fun GetAddressResponse.Transaction.extractDestination(): DestinationType? {
        val address = vout.firstOrNull()?.addresses?.firstOrNull() ?: return null
        val addressType = if (tokenTransfers.isEmpty()) AddressType.User(address) else AddressType.Contract(address)

        return DestinationType.Single(addressType = addressType)
    }

    private fun GetAddressResponse.Transaction.extractSourceType(): SourceType? {
        val address = vin.firstOrNull()?.addresses?.firstOrNull() ?: return null
        return SourceType.Single(address = address)
    }

    private fun GetAddressResponse.Transaction.TokenTransfer.extractAmount(token: Token): Amount {
        val transferValue = value?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return Amount(token = token, value = transferValue.movePointLeft(decimals))
    }

    private fun String.equalsIgnoreCase(other: String): Boolean {
        return this.equals(other, ignoreCase = true)
    }

    private fun shouldExcludeTransaction(amount: Amount): Boolean {
        return amount.value == null || amount.value.signum() == 0
    }
}