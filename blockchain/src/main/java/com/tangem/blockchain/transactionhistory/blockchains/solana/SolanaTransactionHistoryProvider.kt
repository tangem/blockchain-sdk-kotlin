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
            val queryAddress = resolveSignaturesAddress(walletAddress = address, filterType = filterType)
                ?: return TransactionHistoryState.Success.Empty
            val signatures = withContext(Dispatchers.IO) {
                api.getSignaturesForAddress(address = queryAddress, limit = 1)
            }
            if (signatures.isNotEmpty()) {
                TransactionHistoryState.Success.HasTransactions(signatures.size)
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
            val beforeCursor = request.pageToLoad

            val queryAddress = resolveSignaturesAddress(request.address, request.filterType)
                ?: return Result.Success(PaginationWrapper(nextPage = Page.LastPage, items = emptyList()))

            val signatures = withContext(Dispatchers.IO) {
                api.getSignaturesForAddress(
                    address = queryAddress,
                    limit = request.pageSize,
                    before = beforeCursor,
                )
            }

            val items = coroutineScope {
                signatures.map { sigInfo ->
                    async(Dispatchers.IO) {
                        val txResponse = api.getTransaction(sigInfo.signature)
                            ?: return@async null

                        mapper.mapToHistoryItem(
                            signatureInfo = sigInfo,
                            txResponse = txResponse,
                            walletAddress = request.address,
                            filterToken = filterToken,
                        )
                    }
                }.awaitAll().filterNotNull()
            }

            val nextPage = if (signatures.size < request.pageSize) {
                Page.LastPage
            } else {
                Page.Next(signatures.last().signature)
            }

            Result.Success(PaginationWrapper(nextPage = nextPage, items = items))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    /**
     * For SPL tokens, resolves the token account address via `getTokenAccountsByOwner` RPC call.
     * This returns the actual on-chain token account for the wallet+mint pair, ensuring that
     * `getSignaturesForAddress` returns only transactions relevant to that specific token.
     *
     * Returns `null` if the token account doesn't exist (no history available).
     */
    private suspend fun resolveSignaturesAddress(
        walletAddress: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): String? {
        val contract = filterType as? TransactionHistoryRequest.FilterType.Contract
            ?: return walletAddress

        return withContext(Dispatchers.IO) {
            api.getTokenAccountsByOwner(
                owner = walletAddress,
                mint = contract.tokenInfo.contractAddress,
            )
        }
    }
}