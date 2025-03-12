package com.tangem.blockchain.transactionhistory.blockchains.ethereum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EthereumTransactionHistoryProvider(
    blockchain: Blockchain,
    private val blockBookApi: BlockBookApi,
) : TransactionHistoryProvider {

    private val mapper = EthereumTransactionHistoryItemMapper(blockchain)

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                blockBookApi.getTransactions(
                    address = address,
                    page = null,
                    pageSize = 1, // We don't need to know all transactions to define state
                    filterType = filterType,
                )
            }
            if (!response.transactions.isNullOrEmpty()) {
                TransactionHistoryState.Success.HasTransactions(response.transactions.size)
            } else {
                TransactionHistoryState.Success.Empty
            }
        } catch (e: Exception) {
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        return try {
            val response = withContext(Dispatchers.IO) {
                blockBookApi.getTransactions(
                    address = request.address,
                    page = request.pageToLoad,
                    pageSize = request.pageSize,
                    filterType = request.filterType,
                )
            }
            val txs = mapper.convert(
                walletAddress = request.address,
                filterType = request.filterType,
                response = response,
            )
            val nextPage = if (response.page != null && request.page !is Page.LastPage) {
                val page = response.page
                if (page == response.totalPages) Page.LastPage else Page.Next(page.inc().toString())
            } else {
                Page.LastPage
            }
            Result.Success(
                PaginationWrapper(
                    nextPage = nextPage,
                    items = txs,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}