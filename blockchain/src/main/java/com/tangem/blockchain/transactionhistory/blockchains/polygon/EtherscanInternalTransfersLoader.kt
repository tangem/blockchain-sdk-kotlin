package com.tangem.blockchain.transactionhistory.blockchains.polygon

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider.Companion.shouldExcludeFromHistory
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherScanApi
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherscanInternalResult
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherscanInternalTransaction
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherscanInternalTransactionsResponse
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonScanResult
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransaction
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransactionHistoryResponse
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * The coin history of an address together with the internal CALLs that moved its coin: `txlist` and `txlistinternal`
 * page independently, so both are asked up to one `endblock` and merged by [EtherscanBlockCursorMerger].
 */
internal class EtherscanInternalTransfersLoader(
    private val blockchain: Blockchain,
    private val api: EtherScanApi,
    private val apiKey: String,
) {

    suspend fun loadPage(
        request: TransactionHistoryRequest,
        address: String,
        mapTransaction: (PolygonTransaction) -> TransactionHistoryItem?,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        val endBlock = when (val page = request.page) {
            Page.Initial -> EtherScanApi.DEFAULT_END_BLOCK
            is Page.Next -> page.value.toLong()
            Page.LastPage -> error("EOF reached. No data to load")
        }
        val (txRecords, internalRecords) = inParallel(
            first = { loadTransactions(address = address, page = 1, offset = request.pageSize, endBlock = endBlock) },
            second = { loadInternals(address = address, page = 1, offset = request.pageSize, endBlock = endBlock) },
        )
        val merged = EtherscanBlockCursorMerger.merge(
            listOf(
                BlockStream(
                    items = txRecords.toEntries(mapTransaction),
                    isFull = txRecords.size >= request.pageSize,
                ),
                BlockStream(
                    items = internalRecords.toEntries(address),
                    isFull = internalRecords.size >= request.pageSize,
                ),
            ),
        )
        val entries = merged.truncatedBlock?.let { block ->
            val (blockTxs, blockInternals) = loadWholeBlock(address, block)
            blockTxs.toEntries(mapTransaction) + blockInternals.toEntries(address)
        } ?: merged.items
        val items = entries
            .mapNotNull(BlockEntry<TransactionHistoryItem?>::item)
            .filterNot { item -> shouldExcludeFromHistory(filterType = request.filterType, item = item) }

        return Result.Success(
            PaginationWrapper(
                nextPage = merged.nextEndBlock?.let { Page.Next(it.toString()) } ?: Page.LastPage,
                items = items,
            ),
        )
    }

    /** A Safe whose only operation is the deploy bundle is in no `txlist` record, only in the internal list */
    suspend fun hasAny(address: String): Boolean = withContext(Dispatchers.IO) {
        loadTransactions(address, page = 1, offset = 1).isNotEmpty() || hasEligibleInternal(address)
    }

    /** CREATE and zero-value calls are no history items, so the list is scanned past them */
    private suspend fun hasEligibleInternal(address: String): Boolean {
        for (page in 1..MAX_RESULT_WINDOW / LARGE_PAGE_SIZE) {
            val records = loadInternals(address, page = page, offset = LARGE_PAGE_SIZE)
            if (records.any { it.toHistoryItem(address)?.isCoinHistoryItem() == true }) return true
            if (records.size < LARGE_PAGE_SIZE) return false
        }
        return false
    }

    /** Both lists paged within [block] until each is exhausted; Etherscan serves at most [MAX_RESULT_WINDOW] records */
    private suspend fun loadWholeBlock(
        address: String,
        block: Long,
    ): Pair<List<PolygonTransaction>, List<EtherscanInternalTransaction>> = inParallel(
        first = {
            loadAllPages { page ->
                loadTransactions(
                    address = address,
                    page = page,
                    offset = LARGE_PAGE_SIZE,
                    startBlock = block,
                    endBlock = block,
                )
            }
        },
        second = {
            loadAllPages { page ->
                loadInternals(
                    address = address,
                    page = page,
                    offset = LARGE_PAGE_SIZE,
                    startBlock = block,
                    endBlock = block,
                )
            }
        },
    )

    private suspend fun <A, B> inParallel(first: suspend () -> A, second: suspend () -> B): Pair<A, B> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val firstResult = async { first() }
                val secondResult = async { second() }
                firstResult.await() to secondResult.await()
            }
        }

    private suspend fun <T> loadAllPages(loadPage: suspend (page: Int) -> List<T>): List<T> {
        val records = mutableListOf<T>()
        for (page in 1..MAX_RESULT_WINDOW / LARGE_PAGE_SIZE) {
            val pageRecords = loadPage(page)
            records += pageRecords
            if (pageRecords.size < LARGE_PAGE_SIZE) break
        }
        return records
    }

    private suspend fun loadTransactions(
        address: String,
        page: Int,
        offset: Int,
        startBlock: Long = 0,
        endBlock: Long = EtherScanApi.DEFAULT_END_BLOCK,
    ): List<PolygonTransaction> = api.getCoinTransactionHistory(
        chainId = chainId(),
        address = address,
        page = page,
        offset = offset,
        apiKey = apiKey,
        startBlock = startBlock,
        endBlock = endBlock,
    ).requireTransactions()

    private suspend fun loadInternals(
        address: String,
        page: Int,
        offset: Int,
        startBlock: Long = 0,
        endBlock: Long = EtherScanApi.DEFAULT_END_BLOCK,
    ): List<EtherscanInternalTransaction> = api.getInternalTransactionHistory(
        chainId = chainId(),
        address = address,
        page = page,
        offset = offset,
        apiKey = apiKey,
        startBlock = startBlock,
        endBlock = endBlock,
    ).requireTransactions()

    private fun List<PolygonTransaction>.toEntries(
        mapTransaction: (PolygonTransaction) -> TransactionHistoryItem?,
    ): List<BlockEntry<TransactionHistoryItem?>> = mapNotNull { tx ->
        val block = tx.blockNumber?.toLongOrNull() ?: return@mapNotNull null
        BlockEntry(block = block, item = mapTransaction(tx))
    }

    @JvmName("internalsToEntries")
    private fun List<EtherscanInternalTransaction>.toEntries(
        address: String,
    ): List<BlockEntry<TransactionHistoryItem?>> = map { internal ->
        BlockEntry(block = internal.blockNumber.toLong(), item = internal.toHistoryItem(address))
    }

    private fun TransactionHistoryItem.isCoinHistoryItem(): Boolean =
        !shouldExcludeFromHistory(filterType = TransactionHistoryRequest.FilterType.Coin, item = this)

    private fun chainId(): Int = requireNotNull(blockchain.getChainId()) { "chainId must not be null" }

    /** A CALL that moved coin of [walletAddress]; the gas was paid by whoever sent the transaction */
    private fun EtherscanInternalTransaction.toHistoryItem(walletAddress: String): TransactionHistoryItem? {
        if (type != INTERNAL_CALL_TYPE) return null
        if (!from.equals(walletAddress, ignoreCase = true) && !to.equals(walletAddress, ignoreCase = true)) return null
        val wei = value.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null

        return TransactionHistoryItem(
            txHash = hash,
            timestamp = TimeUnit.SECONDS.toMillis(timeStamp.toLong()),
            isOutgoing = from.equals(walletAddress, ignoreCase = true),
            destinationType = TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(address = to),
            ),
            sourceType = TransactionHistoryItem.SourceType.Single(address = from),
            status = if (isError == TRUE_FLAG) {
                TransactionHistoryItem.TransactionStatus.Failed
            } else {
                TransactionHistoryItem.TransactionStatus.Confirmed
            },
            type = TransactionHistoryItem.TransactionType.Transfer,
            amount = Amount(value = wei.movePointLeft(blockchain.decimals()), blockchain = blockchain),
            fee = Amount(value = BigDecimal.ZERO, blockchain = blockchain),
        )
    }

    /** An exhausted list answers `status = 0` with an empty array; any other non-success is an error */
    @Suppress("TooGenericExceptionThrown")
    private fun PolygonTransactionHistoryResponse.requireTransactions(): List<PolygonTransaction> =
        when (val result = result) {
            is PolygonScanResult.Transactions -> if (status == TRUE_FLAG || result.txs.isEmpty()) {
                result.txs
            } else {
                throw Exception(message)
            }
            is PolygonScanResult.Description -> throw Exception(result.description)
        }

    @Suppress("TooGenericExceptionThrown")
    private fun EtherscanInternalTransactionsResponse.requireTransactions(): List<EtherscanInternalTransaction> =
        when (val result = result) {
            is EtherscanInternalResult.Transactions -> if (status == TRUE_FLAG || result.txs.isEmpty()) {
                result.txs
            } else {
                throw Exception(message)
            }
            is EtherscanInternalResult.Description -> throw Exception(result.description)
        }

    private companion object {
        const val INTERNAL_CALL_TYPE = "call"

        /** Etherscan answers only while `page * offset` stays within it */
        const val MAX_RESULT_WINDOW = 10_000
        const val LARGE_PAGE_SIZE = 1_000

        /** Etherscan's boolean `true`: the `status` of a successful response, the `isError` of a failed call */
        const val TRUE_FLAG = "1"
    }
}