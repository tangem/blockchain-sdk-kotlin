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
            val queryAddress = resolveQueryAddress(walletAddress = address, filterType = filterType)
                ?: return TransactionHistoryState.Success.Empty
            val response = withContext(Dispatchers.IO) {
                api.getTransactionsForAddress(address = queryAddress, limit = 1)
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

            val queryAddress = resolveQueryAddress(
                walletAddress = request.address,
                filterType = request.filterType,
            ) ?: return Result.Success(PaginationWrapper(nextPage = Page.LastPage, items = emptyList()))

            val response = withContext(Dispatchers.IO) {
                api.getTransactionsForAddress(
                    address = queryAddress,
                    limit = request.pageSize,
                    paginationToken = request.pageToLoad,
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

    private suspend fun resolveQueryAddress(walletAddress: String, filterType: FilterType): String? {
        val contract = filterType as? FilterType.Contract ?: return walletAddress
        return withContext(Dispatchers.IO) {
            api.getTokenAccountsByOwner(
                owner = walletAddress,
                mint = contract.tokenInfo.contractAddress,
            )
        }
    }
}