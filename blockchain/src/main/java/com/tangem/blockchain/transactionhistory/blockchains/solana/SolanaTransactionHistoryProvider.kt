package com.tangem.blockchain.transactionhistory.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.blockchains.solana.network.SolanaTransactionHistoryApi
import com.tangem.blockchain.transactionhistory.blockchains.solana.network.SolanaTransactionHistoryApi.TokenAccountsFilter
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest.FilterType
import kotlinx.coroutines.*

internal class SolanaTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val rpcClient: SolanaRpcClient,
) : TransactionHistoryProvider {

    private val api = SolanaTransactionHistoryApi(rpcClient)
    private val mapper = SolanaTransactionHistoryMapper(blockchain)

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                api.getTransactionsForAddress(
                    address = address,
                    limit = 1,
                    tokenAccountsFilter = filterType.toTokenAccountsFilter(),
                )
            }
            if (response.data.isNotEmpty()) {
                TransactionHistoryState.Success.HasTransactions(response.data.size)
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
            val filterToken =
                (request.filterType as? TransactionHistoryRequest.FilterType.Contract)?.tokenInfo

            val response = withContext(Dispatchers.IO) {
                api.getTransactionsForAddress(
                    address = request.address,
                    limit = request.pageSize,
                    paginationToken = request.pageToLoad,
                    tokenAccountsFilter = request.filterType.toTokenAccountsFilter(),
                )
            }

            val items = response.data.mapNotNull { txResponse ->
                mapper.mapToHistoryItem(
                    txResponse = txResponse,
                    walletAddress = request.address,
                    filterToken = filterToken,
                )
            }

            val nextPage = response.paginationToken
                ?.let { Page.Next(it) }
                ?: Page.LastPage

            Result.Success(PaginationWrapper(nextPage = nextPage, items = items))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun FilterType.toTokenAccountsFilter(): TokenAccountsFilter {
        return when (this) {
            is FilterType.Contract -> TokenAccountsFilter.BalanceChanged
            FilterType.Coin -> TokenAccountsFilter.Default
        }
    }
}