package com.tangem.blockchain.transactionhistory.blockchains.polygon

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherScanApi
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherscanInternalResult
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherscanInternalTransaction
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherscanInternalTransactionsResponse
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonScanResult
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransaction
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransactionHistoryResponse
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

class EtherscanTransactionHistoryProviderTest {

    private val api = mockk<EtherScanApi>()
    private val provider = EtherscanTransactionHistoryProvider(
        blockchain = Blockchain.Polygon,
        api = api,
        etherscanApiKey = API_KEY,
    )
    private val monadProvider = EtherscanTransactionHistoryProvider(
        blockchain = Blockchain.Monad,
        api = api,
        etherscanApiKey = API_KEY,
    )

    @Test
    fun `internal calls of the Safe are merged into its coin history and the relayer call is kept as before`() =
        runTest {
            stubPage(
                endBlock = EtherScanApi.DEFAULT_END_BLOCK,
                txs = listOf(relayerCall(block = 94_213_311)),
                internals = listOf(
                    internal(block = 94_213_311, to = RELAYER, value = "33203274238597392"),
                    internal(block = 94_213_311, to = RECIPIENT, value = "10935223680498695559"),
                ),
            )

            val page = loadSafePage(Page.Initial)

            assertThat(page.items.filter { it.type == TransactionHistoryItem.TransactionType.Transfer })
                .containsExactly(
                    safeTransfer(to = RELAYER, wei = "33203274238597392"),
                    safeTransfer(to = RECIPIENT, wei = "10935223680498695559"),
                ).inOrder()
            assertThat(page.nextPage).isEqualTo(Page.LastPage)
        }

    @Test
    fun `create2, zero-value, foreign and failed internal calls are filtered or marked`() = runTest {
        val internals = listOf(
            internal(block = 94_206_175, from = FACTORY, to = "", value = "0", type = "create2"),
            internal(block = 94_206_175, to = RECIPIENT, value = "0"),
            internal(block = 94_206_175, from = OTHER, to = RECIPIENT, value = "1"),
            internal(block = 94_206_175, to = RECIPIENT, value = "1093522368049869555", isError = "1"),
        )
        stubPage(endBlock = EtherScanApi.DEFAULT_END_BLOCK, txs = emptyList(), internals = internals)
        stubWholeBlock(block = 94_206_175, internals = internals)

        val page = loadSafePage(Page.Initial)

        assertThat(page.items).containsExactly(
            safeTransfer(
                to = RECIPIENT,
                wei = "1093522368049869555",
                status = TransactionHistoryItem.TransactionStatus.Failed,
            ),
        )
    }

    @Test
    fun `a full internal list moves the cursor to its boundary block`() = runTest {
        stubPage(
            endBlock = EtherScanApi.DEFAULT_END_BLOCK,
            txs = emptyList(),
            internals = listOf(
                internal(block = 20, to = RECIPIENT, value = "1"),
                internal(block = 15, to = RECIPIENT, value = "2"),
                internal(block = 10, to = RECIPIENT, value = "3"),
            ),
        )

        val page = loadSafePage(Page.Initial)

        assertThat(page.items).containsExactly(
            safeTransfer(to = RECIPIENT, wei = "1"),
            safeTransfer(to = RECIPIENT, wei = "2"),
        ).inOrder()
        assertThat(page.nextPage).isEqualTo(Page.Next("10"))
    }

    @Test
    fun `the next page asks both lists up to the cursor block`() = runTest {
        stubPage(
            endBlock = 10,
            txs = emptyList(),
            internals = listOf(internal(block = 10, to = RECIPIENT, value = "2")),
        )

        loadSafePage(Page.Next("10"))

        coVerify { api.getCoinTransactionHistory(CHAIN_ID, SAFE, 1, PAGE_SIZE, API_KEY, endBlock = 10) }
        coVerify { api.getInternalTransactionHistory(CHAIN_ID, SAFE, 1, PAGE_SIZE, API_KEY, endBlock = 10) }
    }

    @Test
    fun `a page that is all one block is fetched whole before the cursor moves below it`() = runTest {
        // More internal calls in block 20 than a page holds: the first page shows only part of them
        stubPage(
            endBlock = EtherScanApi.DEFAULT_END_BLOCK,
            txs = emptyList(),
            internals = listOf(
                internal(block = 20, to = RECIPIENT, value = "1"),
                internal(block = 20, to = RECIPIENT, value = "2"),
                internal(block = 20, to = RECIPIENT, value = "3"),
            ),
        )
        stubWholeBlock(
            block = 20,
            internals = (1..4).map { internal(block = 20, to = RECIPIENT, value = it.toString()) },
        )

        val page = loadSafePage(Page.Initial)

        assertThat(page.items).containsExactly(
            safeTransfer(to = RECIPIENT, wei = "1"),
            safeTransfer(to = RECIPIENT, wei = "2"),
            safeTransfer(to = RECIPIENT, wei = "3"),
            safeTransfer(to = RECIPIENT, wei = "4"),
        ).inOrder()
        assertThat(page.nextPage).isEqualTo(Page.Next("19"))
    }

    @Test
    fun `a Safe whose only operation is the deploy bundle has history`() = runTest {
        // The bundle is sent to Multicall3, so the Safe is in no txlist record
        stubHistoryState(
            internals = listOf(internal(block = 94_206_175, to = RECIPIENT, value = "1093522368049869555")),
        )

        val state = safeHistoryState()

        assertThat(state).isInstanceOf(TransactionHistoryState.Success.HasTransactions::class.java)
    }

    @Test
    fun `a Safe whose only internal records are a deployment and zero-value calls has no history`() = runTest {
        stubHistoryState(
            internals = listOf(
                internal(block = 94_206_175, from = FACTORY, to = "", value = "0", type = "create2"),
                internal(block = 94_206_175, to = RECIPIENT, value = "0"),
            ),
        )

        val state = safeHistoryState()

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `without the flag txlistinternal is never asked`() = runTest {
        coEvery { api.getCoinTransactionHistory(CHAIN_ID, SAFE, 1, PAGE_SIZE, API_KEY) } returns
            txsResponse(listOf(relayerCall(block = 1)))

        provider.getTransactionsHistory(request(Page.Initial, shouldIncludeInternalTransfers = false))

        coVerify(exactly = 0) {
            api.getInternalTransactionHistory(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `token history drops transfers of other contracts the explorer returned despite the filter`() = runTest {
        coEvery {
            api.getTokenTransactionHistory(
                chainId = CHAIN_ID,
                address = SAFE,
                page = 1,
                offset = PAGE_SIZE,
                apiKey = API_KEY,
                contractAddress = USDC,
            )
        } returns txsResponse(
            listOf(
                tokenTransfer(hash = USDC_TX_HASH, contract = USDC.uppercase(), value = "974416"),
                tokenTransfer(hash = FOREIGN_TX_HASH, contract = FOREIGN_TOKEN, value = "5675307337351213033"),
            ),
        )

        val result = provider.getTransactionsHistory(tokenRequest()) as Result.Success

        assertThat(result.data.items.map { it.txHash to it.amount.value }).containsExactly(
            USDC_TX_HASH to BigDecimal("0.974416"),
        )
    }

    @Test
    fun `a Monad call to an unverified contract is recognised by its selector alone`() = runTest {
        val call = coinTransaction(to = CONTRACT, value = "0", functionName = "", methodId = CLAIM_SELECTOR)
        stubCoinPage(chainId = MONAD_CHAIN_ID, txs = listOf(call))

        val page = loadPage(monadProvider)

        assertThat(page.items).containsExactly(
            coinItem(
                to = TransactionHistoryItem.AddressType.Contract(CONTRACT),
                type = TransactionHistoryItem.TransactionType.ContractMethod(
                    id = CLAIM_SELECTOR,
                    callData = call.input,
                ),
                wei = "0",
                blockchain = Blockchain.Monad,
            ),
        )
    }

    @Test
    fun `a Monad coin transfer to a user with input data stays a transfer`() = runTest {
        val transfer = coinTransaction(to = RECIPIENT, value = "1", functionName = "", methodId = CLAIM_SELECTOR)
        stubCoinPage(chainId = MONAD_CHAIN_ID, txs = listOf(transfer))

        val page = loadPage(monadProvider)

        assertThat(page.items).containsExactly(
            coinItem(
                to = TransactionHistoryItem.AddressType.User(RECIPIENT),
                type = TransactionHistoryItem.TransactionType.Transfer,
                wei = "1",
                blockchain = Blockchain.Monad,
            ),
        )
    }

    @Test
    fun `token history state ignores transfers of other contracts`() = runTest {
        coEvery {
            api.getTokenTransactionHistory(
                chainId = CHAIN_ID,
                address = SAFE,
                page = 1,
                offset = 100,
                apiKey = API_KEY,
                contractAddress = USDC,
            )
        } returns txsResponse(
            listOf(tokenTransfer(hash = FOREIGN_TX_HASH, contract = FOREIGN_TOKEN, value = "5675307337351213033")),
        )

        val state = provider.getTransactionHistoryState(
            address = SAFE,
            filterType = TransactionHistoryRequest.FilterType.Contract(USDC_TOKEN),
        )

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    private fun tokenRequest() = TransactionHistoryRequest(
        address = SAFE,
        decimals = USDC_TOKEN.decimals,
        page = Page.Initial,
        pageSize = PAGE_SIZE,
        filterType = TransactionHistoryRequest.FilterType.Contract(USDC_TOKEN),
    )

    private fun tokenTransfer(hash: String, contract: String, value: String) = PolygonTransaction(
        confirmations = "12",
        contractAddress = contract,
        from = OTHER,
        functionName = null,
        methodId = null,
        gasPrice = "1",
        gasUsed = "1",
        hash = hash,
        isError = null,
        timeStamp = TIMESTAMP,
        to = SAFE,
        txReceiptStatus = null,
        value = value,
        input = "0x",
    )

    @Test
    fun `outside Monad a selector without a function name is not a contract call`() = runTest {
        val zeroValueCall = coinTransaction(to = CONTRACT, value = "0", functionName = "", methodId = CLAIM_SELECTOR)
        val transfer = coinTransaction(to = RECIPIENT, value = "1", functionName = "", methodId = CLAIM_SELECTOR)
        stubCoinPage(chainId = CHAIN_ID, txs = listOf(zeroValueCall, transfer))

        val page = loadPage(provider)

        assertThat(page.items).containsExactly(
            coinItem(
                to = TransactionHistoryItem.AddressType.User(RECIPIENT),
                type = TransactionHistoryItem.TransactionType.Transfer,
                wei = "1",
            ),
        )
    }

    @Test
    fun `a plain coin transfer with an empty selector stays a transfer to the user`() = runTest {
        listOf("0x", "", null).forEach { methodId ->
            val transfer = coinTransaction(to = RECIPIENT, value = "1", functionName = "", methodId = methodId)
            stubCoinPage(chainId = MONAD_CHAIN_ID, txs = listOf(transfer))

            val page = loadPage(monadProvider)

            assertWithMessage("methodId = $methodId").that(page.items).containsExactly(
                coinItem(
                    to = TransactionHistoryItem.AddressType.User(RECIPIENT),
                    type = TransactionHistoryItem.TransactionType.Transfer,
                    wei = "1",
                    blockchain = Blockchain.Monad,
                ),
            )
        }
    }

    private fun stubCoinPage(chainId: Int, txs: List<PolygonTransaction>) {
        coEvery { api.getCoinTransactionHistory(chainId, SAFE, 1, PAGE_SIZE, API_KEY) } returns txsResponse(txs)
    }

    private suspend fun loadPage(
        provider: EtherscanTransactionHistoryProvider,
    ): PaginationWrapper<TransactionHistoryItem> {
        val request = request(Page.Initial, shouldIncludeInternalTransfers = false)
        return (provider.getTransactionsHistory(request) as Result.Success).data
    }

    private fun stubWholeBlock(block: Long, internals: List<EtherscanInternalTransaction>) {
        coEvery {
            api.getCoinTransactionHistory(
                CHAIN_ID,
                SAFE,
                1,
                LARGE_PAGE_SIZE,
                API_KEY,
                startBlock = block,
                endBlock = block,
            )
        } returns noTransactions()
        coEvery {
            api.getInternalTransactionHistory(
                CHAIN_ID,
                SAFE,
                1,
                LARGE_PAGE_SIZE,
                API_KEY,
                startBlock = block,
                endBlock = block,
            )
        } returns internalsResponse(internals)
    }

    private fun stubHistoryState(internals: List<EtherscanInternalTransaction>) {
        coEvery { api.getCoinTransactionHistory(CHAIN_ID, SAFE, 1, 1, API_KEY) } returns noTransactions()
        coEvery { api.getInternalTransactionHistory(CHAIN_ID, SAFE, 1, LARGE_PAGE_SIZE, API_KEY) } returns
            internalsResponse(internals)
    }

    private suspend fun safeHistoryState() = provider.getTransactionHistoryState(
        address = SAFE,
        filterType = TransactionHistoryRequest.FilterType.Coin,
        shouldIncludeInternalTransfers = true,
    )

    private suspend fun loadSafePage(page: Page) =
        (provider.getTransactionsHistory(request(page, shouldIncludeInternalTransfers = true)) as Result.Success).data

    private fun request(page: Page, shouldIncludeInternalTransfers: Boolean) = TransactionHistoryRequest(
        address = SAFE,
        decimals = Blockchain.Polygon.decimals(),
        page = page,
        pageSize = PAGE_SIZE,
        filterType = TransactionHistoryRequest.FilterType.Coin,
        shouldIncludeInternalTransfers = shouldIncludeInternalTransfers,
    )

    private fun stubPage(endBlock: Long, txs: List<PolygonTransaction>, internals: List<EtherscanInternalTransaction>) {
        coEvery { api.getCoinTransactionHistory(CHAIN_ID, SAFE, 1, PAGE_SIZE, API_KEY, endBlock = endBlock) } returns
            if (txs.isEmpty()) noTransactions() else txsResponse(txs)
        coEvery {
            api.getInternalTransactionHistory(CHAIN_ID, SAFE, 1, PAGE_SIZE, API_KEY, endBlock = endBlock)
        } returns internalsResponse(internals)
    }

    private fun txsResponse(txs: List<PolygonTransaction>) = PolygonTransactionHistoryResponse(
        status = "1",
        message = "OK",
        result = PolygonScanResult.Transactions(txs),
    )

    private fun noTransactions() = PolygonTransactionHistoryResponse(
        status = "0",
        message = "No transactions found",
        result = PolygonScanResult.Transactions(emptyList()),
    )

    private fun internalsResponse(internals: List<EtherscanInternalTransaction>) =
        EtherscanInternalTransactionsResponse(
            status = if (internals.isEmpty()) "0" else "1",
            message = if (internals.isEmpty()) "No transactions found" else "OK",
            result = EtherscanInternalResult.Transactions(internals),
        )

    private fun relayerCall(block: Long) = PolygonTransaction(
        confirmations = "12",
        contractAddress = "",
        from = RELAYER,
        functionName = "execTransaction(address to,uint256 value,bytes data,uint8 operation)",
        methodId = "0x6a761202",
        gasPrice = "1",
        gasUsed = "1",
        hash = TX_HASH,
        isError = "0",
        timeStamp = TIMESTAMP,
        to = SAFE,
        txReceiptStatus = "1",
        value = "0",
        input = "0x6a761202",
        blockNumber = block.toString(),
    )

    private fun coinTransaction(to: String, value: String, functionName: String, methodId: String?) =
        PolygonTransaction(
            confirmations = "12",
            contractAddress = "",
            from = SAFE,
            functionName = functionName,
            methodId = methodId,
            gasPrice = "2",
            gasUsed = "3",
            hash = TX_HASH,
            isError = "0",
            timeStamp = TIMESTAMP,
            to = to,
            txReceiptStatus = "1",
            value = value,
            input = if (methodId?.removePrefix("0x").isNullOrEmpty()) "0x" else "${methodId}0000",
            blockNumber = "1",
        )

    private fun coinItem(
        to: TransactionHistoryItem.AddressType,
        type: TransactionHistoryItem.TransactionType,
        wei: String,
        blockchain: Blockchain = Blockchain.Polygon,
    ) = TransactionHistoryItem(
        txHash = TX_HASH,
        timestamp = TIMESTAMP.toLong() * MILLIS_IN_SECOND,
        isOutgoing = true,
        destinationType = TransactionHistoryItem.DestinationType.Single(to),
        sourceType = TransactionHistoryItem.SourceType.Single(SAFE),
        status = TransactionHistoryItem.TransactionStatus.Confirmed,
        type = type,
        amount = Amount(
            value = BigDecimal(wei).movePointLeft(blockchain.decimals()),
            blockchain = blockchain,
            type = AmountType.Coin,
        ),
        fee = Amount(
            value = BigDecimal(6).movePointLeft(blockchain.decimals()),
            blockchain = blockchain,
        ),
    )

    @Suppress("LongParameterList")
    private fun internal(
        block: Long,
        to: String,
        value: String,
        from: String = SAFE,
        type: String = "call",
        isError: String = "0",
    ) = EtherscanInternalTransaction(
        blockNumber = block.toString(),
        timeStamp = TIMESTAMP,
        hash = TX_HASH,
        from = from,
        to = to,
        value = value,
        type = type,
        isError = isError,
    )

    private fun safeTransfer(
        to: String,
        wei: String,
        status: TransactionHistoryItem.TransactionStatus = TransactionHistoryItem.TransactionStatus.Confirmed,
    ) = TransactionHistoryItem(
        txHash = TX_HASH,
        timestamp = TIMESTAMP.toLong() * MILLIS_IN_SECOND,
        isOutgoing = true,
        destinationType = TransactionHistoryItem.DestinationType.Single(TransactionHistoryItem.AddressType.User(to)),
        sourceType = TransactionHistoryItem.SourceType.Single(SAFE),
        status = status,
        type = TransactionHistoryItem.TransactionType.Transfer,
        amount = Amount(
            value = BigDecimal(wei).movePointLeft(Blockchain.Polygon.decimals()),
            blockchain = Blockchain.Polygon,
            type = AmountType.Coin,
        ),
        fee = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Polygon),
    )

    private companion object {
        const val API_KEY = "key"
        const val CHAIN_ID = 137
        const val PAGE_SIZE = 3
        const val LARGE_PAGE_SIZE = 1_000
        const val MILLIS_IN_SECOND = 1000
        const val SAFE = "0x4cd2b9a4197dd56e8bde666691a18b2bfa9d3c74"
        const val RELAYER = "0x5f2c435df8d00d0cf842a52dd166b1dcd69734c2"
        const val RECIPIENT = "0x6dbd0c1957dfe9d85f74b8fe3a355b268a39ec91"
        const val FACTORY = "0x14f2982d601c9458f93bd70b218933a6f8165e7b"
        const val OTHER = "0x2c6e7c809ac1adf71c6dd0d67d488d2938eff69a"
        const val CONTRACT = "0xcb522fef86463a665bd5493100fca7396786c572"
        const val CLAIM_SELECTOR = "0xb4701e3e"
        val MONAD_CHAIN_ID = requireNotNull(Blockchain.Monad.getChainId())
        const val TX_HASH = "0x55233c9ef541232c1a94f8b0806401e4e5188930ff0c795ffa51e96a92e53da0"
        const val TIMESTAMP = "1790025057"
        const val USDC = "0x3355df6d4c9c3035724fd0e3914de96a5a83aaf4"
        const val FOREIGN_TOKEN = "0x5d0d7bca050e2e98fd4a5e8d3ba823b49f39868d"
        const val USDC_TX_HASH = "0x1dc19f03052e252d05610b4744b103cb8bc23912908ee6ff85071cd166c2614d"
        const val FOREIGN_TX_HASH = "0x00215c0e850598a99fe01af2e0d3c0cb7b06037ce62b557f3cc94e5ee1c530b8"
        val USDC_TOKEN = Token(symbol = "USDC.e", contractAddress = USDC, decimals = 6)
    }
}