package com.tangem.blockchain.transactionhistory.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryRequest
import com.tangem.blockchain.common.txhistory.TransactionHistoryState
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr

internal class KoinosTransactionHistoryProvider(
    private val networkService: KoinosNetworkService,
) : TransactionHistoryProvider {
    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        val nonceResult = networkService.getCurrentNonce(address)
            .successOr { return TransactionHistoryState.Failed.FetchError(exception = it.error) }

        // TODO mb change txCount type to BigInteger? ([REDACTED_TASK_KEY])
        return TransactionHistoryState.Success.HasTransactions(txCount = nonceResult.nonce.toInt())
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        // TODO [REDACTED_TASK_KEY]
        return Result.Failure(BlockchainSdkError.WrappedThrowable(NotImplementedError()))
    }
}