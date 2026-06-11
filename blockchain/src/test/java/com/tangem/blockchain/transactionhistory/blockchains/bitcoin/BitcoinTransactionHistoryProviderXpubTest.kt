package com.tangem.blockchain.transactionhistory.blockchains.bitcoin

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetXpubResponse
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Tests for the dynamic-addresses (XPUB) transaction-history path of [BitcoinTransactionHistoryProvider].
 * Fixtures are taken from real Dogecoin BlockBook `?details=txslight` responses.
 *
 * The key distinction from the single-address path is ownership resolution via the `isOwn` flag,
 * which lets internal transfers between own derived addresses be classified correctly.
 */
class BitcoinTransactionHistoryProviderXpubTest {

    private val blockBookApi = mockk<BlockBookApi>()
    private val provider = BitcoinTransactionHistoryProvider(
        blockchain = Blockchain.Dogecoin,
        blockBookApi = blockBookApi,
    )

    @Before
    fun setup() {
        // android.util.Log is not available in JVM unit tests
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    // region getTransactionsHistoryByXpub

    @Test
    fun `incoming transaction is classified by ownership`() = runTest {
        // vin: external; vout[0] ours (received), vout[1] sender's change
        val tx = tx(
            txid = "64c9f5ad",
            fees = "1920000",
            vin = listOf(vin(SENDER, "110000000", isOwn = null)),
            vout = listOf(
                vout(OWN_MAIN, "100000000", isOwn = true),
                vout(SENDER_CHANGE, "8080000", isOwn = null),
            ),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.isOutgoing).isFalse()
        // received = own vout = 1.0 DOGE
        assertThat(item.amount.value!!.compareTo(BigDecimal("1"))).isEqualTo(0)
        assertThat(item.sourceType).isEqualTo(TransactionHistoryItem.SourceType.Single(SENDER))
    }

    @Test
    fun `outgoing transaction amount is external outputs plus fee`() = runTest {
        // vin ours; vout[0] external recipient, vout[1] our change
        val tx = tx(
            txid = "eebb730e",
            fees = "2208020",
            vin = listOf(vin(OWN_MAIN, "294391980", isOwn = true)),
            vout = listOf(
                vout(RECIPIENT, "100000000", isOwn = null),
                vout(OWN_MAIN, "192183960", isOwn = true),
            ),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.isOutgoing).isTrue()
        // sent = external 100000000 + fee 2208020 = 1.0220802 DOGE
        assertThat(item.amount.value!!.compareTo(BigDecimal("1.0220802"))).isEqualTo(0)
        assertThat(item.destinationType).isEqualTo(
            TransactionHistoryItem.DestinationType.Single(TransactionHistoryItem.AddressType.User(RECIPIENT)),
        )
    }

    @Test
    fun `self-transfer between own addresses counts only the fee`() = runTest {
        // The bug this fixes: vout[0] is our own derived receive address (isOwn), not external.
        // Single-address logic would wrongly treat it as a 1.0 DOGE outgoing payment.
        val tx = tx(
            txid = "c380fcf9",
            fees = "1920000",
            vin = listOf(vin(OWN_MAIN, "1346738351", isOwn = true)),
            vout = listOf(
                vout(OWN_RECEIVE, "100000000", isOwn = true),
                vout(OWN_MAIN, "1244818351", isOwn = true),
            ),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.isOutgoing).isTrue()
        // only the fee is spent: 1920000 -> 0.0192 DOGE
        assertThat(item.amount.value!!.compareTo(BigDecimal("0.0192"))).isEqualTo(0)
    }

    @Test
    fun `self-transfer source and destination are own addresses`() = runTest {
        val tx = tx(
            txid = "c380fcf9",
            fees = "1920000",
            vin = listOf(vin(OWN_MAIN, "1346738351", isOwn = true)),
            vout = listOf(
                vout(OWN_RECEIVE, "100000000", isOwn = true),
                vout(OWN_MAIN, "1244818351", isOwn = true),
            ),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        // No external counterparty: both ends resolve to our own (first own) addresses.
        assertThat(item.sourceType).isEqualTo(TransactionHistoryItem.SourceType.Single(OWN_MAIN))
        assertThat(item.destinationType).isEqualTo(
            TransactionHistoryItem.DestinationType.Single(TransactionHistoryItem.AddressType.User(OWN_RECEIVE)),
        )
    }

    @Test
    fun `multiple external inputs and outputs produce Multiple source and destination`() = runTest {
        val tx = tx(
            txid = "multi",
            fees = "1000000",
            vin = listOf(
                vin(SENDER, "500000000", isOwn = null),
                vin(SENDER_CHANGE, "500000000", isOwn = null),
            ),
            vout = listOf(
                vout(OWN_MAIN, "100000000", isOwn = true),
                vout(RECIPIENT, "200000000", isOwn = null),
                vout(RECIPIENT2, "300000000", isOwn = null),
            ),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.isOutgoing).isFalse()
        val source = item.sourceType as TransactionHistoryItem.SourceType.Multiple
        assertThat(source.addresses).containsExactly(SENDER, SENDER_CHANGE)
        val destination = item.destinationType as TransactionHistoryItem.DestinationType.Multiple
        assertThat(destination.addressTypes.map { it.address }).containsExactly(RECIPIENT, RECIPIENT2)
    }

    @Test
    fun `mixed own and external inputs is outgoing with external source`() = runTest {
        val tx = tx(
            txid = "mixed",
            fees = "1000000",
            vin = listOf(
                vin(OWN_MAIN, "100000000", isOwn = true),
                vin(SENDER, "100000000", isOwn = null),
            ),
            vout = listOf(vout(RECIPIENT, "199000000", isOwn = null)),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        // Any own input => outgoing; source reflects the non-own inputs.
        assertThat(item.isOutgoing).isTrue()
        assertThat(item.sourceType).isEqualTo(TransactionHistoryItem.SourceType.Single(SENDER))
    }

    @Test
    fun `all isOwn null yields incoming with zero amount`() = runTest {
        val tx = tx(
            txid = "noflags",
            fees = "1000000",
            vin = listOf(vin(SENDER, "100000000", isOwn = null)),
            vout = listOf(vout(RECIPIENT, "99000000", isOwn = null)),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.isOutgoing).isFalse()
        assertThat(item.amount.value!!.compareTo(BigDecimal.ZERO)).isEqualTo(0)
    }

    @Test
    fun `unconfirmed transaction has Unconfirmed status`() = runTest {
        val tx = tx(
            txid = "pending",
            fees = "1000000",
            confirmations = 0,
            vin = listOf(vin(SENDER, "100000000", isOwn = null)),
            vout = listOf(vout(OWN_MAIN, "100000000", isOwn = true)),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.status).isEqualTo(TransactionHistoryItem.TransactionStatus.Unconfirmed)
    }

    @Test
    fun `item maps hash timestamp fee and type`() = runTest {
        val tx = tx(
            txid = "hash123",
            fees = "1920000",
            blockTime = 1_700_000_000,
            vin = listOf(vin(OWN_MAIN, "294391980", isOwn = true)),
            vout = listOf(vout(RECIPIENT, "100000000", isOwn = null)),
        )
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns xpubResponse(listOf(tx), page = 1)

        val item = singleItem()

        assertThat(item.txHash).isEqualTo("hash123")
        assertThat(item.timestamp).isEqualTo(1_700_000_000L * 1000)
        assertThat(item.type).isEqualTo(TransactionHistoryItem.TransactionType.Transfer)
        // fee = 1920000 -> 0.0192 DOGE
        assertThat(item.fee.value!!.compareTo(BigDecimal("0.0192"))).isEqualTo(0)
    }

    @Test
    fun `next page is built from page and totalPages`() = runTest {
        val tx = tx(txid = "a", fees = "0", vin = emptyList(), vout = emptyList())
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns
            xpubResponse(listOf(tx), page = 1, totalPages = 2)

        val result = provider.getTransactionsHistoryByXpub(DESCRIPTOR, request(Page.Initial))

        val nextPage = (result as Result.Success).data.nextPage
        assertThat(nextPage).isEqualTo(Page.Next("2"))
    }

    @Test
    fun `last page returns LastPage`() = runTest {
        val tx = tx(txid = "a", fees = "0", vin = emptyList(), vout = emptyList())
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, 2, PAGE_SIZE) } returns
            xpubResponse(listOf(tx), page = 2, totalPages = 2)

        val result = provider.getTransactionsHistoryByXpub(DESCRIPTOR, request(Page.Next("2")))

        val nextPage = (result as Result.Success).data.nextPage
        assertThat(nextPage).isEqualTo(Page.LastPage)
    }

    @Test
    fun `null totalPages returns LastPage`() = runTest {
        val tx = tx(txid = "a", fees = "0", vin = emptyList(), vout = emptyList())
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns
            xpubResponse(listOf(tx), page = 1, totalPages = null)

        val result = provider.getTransactionsHistoryByXpub(DESCRIPTOR, request(Page.Initial))

        val nextPage = (result as Result.Success).data.nextPage
        assertThat(nextPage).isEqualTo(Page.LastPage)
    }

    @Test
    fun `null response page returns LastPage`() = runTest {
        val tx = tx(txid = "a", fees = "0", vin = emptyList(), vout = emptyList())
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } returns
            xpubResponse(listOf(tx), page = null, totalPages = 3)

        val result = provider.getTransactionsHistoryByXpub(DESCRIPTOR, request(Page.Initial))

        val nextPage = (result as Result.Success).data.nextPage
        assertThat(nextPage).isEqualTo(Page.LastPage)
    }

    @Test
    fun `api failure returns Failure`() = runTest {
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, PAGE_SIZE) } throws IllegalStateException("network down")

        val result = provider.getTransactionsHistoryByXpub(DESCRIPTOR, request(Page.Initial))

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    // endregion

    // region getTransactionHistoryStateByXpub

    @Test
    fun `state returns HasTransactions when transactions present`() = runTest {
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, 1) } returns
            xpubResponse(listOf(tx(txid = "a", fees = "0", vin = emptyList(), vout = emptyList())), page = 1, txs = 5)

        val state = provider.getTransactionHistoryStateByXpub(DESCRIPTOR)

        assertThat(state).isInstanceOf(TransactionHistoryState.Success.HasTransactions::class.java)
        assertThat((state as TransactionHistoryState.Success.HasTransactions).txCount).isEqualTo(5)
    }

    @Test
    fun `state returns Empty when no transactions`() = runTest {
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, 1) } returns xpubResponse(transactions = emptyList())

        val state = provider.getTransactionHistoryStateByXpub(DESCRIPTOR)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `state returns FetchError when api throws`() = runTest {
        coEvery { blockBookApi.getXpubInfo(DESCRIPTOR, null, 1) } throws IllegalStateException("boom")

        val state = provider.getTransactionHistoryStateByXpub(DESCRIPTOR)

        assertThat(state).isInstanceOf(TransactionHistoryState.Failed.FetchError::class.java)
    }

    // endregion

    // region helpers

    private suspend fun singleItem(): TransactionHistoryItem {
        val result = provider.getTransactionsHistoryByXpub(DESCRIPTOR, request(Page.Initial))
        return (result as Result.Success).data.items.single()
    }

    private fun request(page: Page) = TransactionHistoryRequest(
        address = OWN_MAIN,
        decimals = Blockchain.Dogecoin.decimals(),
        page = page,
        pageSize = PAGE_SIZE,
        filterType = TransactionHistoryRequest.FilterType.Coin,
    )

    @Suppress("LongParameterList")
    private fun xpubResponse(
        transactions: List<GetAddressResponse.Transaction>?,
        page: Int? = null,
        totalPages: Int? = null,
        txs: Int = transactions?.size ?: 0,
    ) = GetXpubResponse(
        page = page,
        totalPages = totalPages,
        itemsOnPage = null,
        address = DESCRIPTOR,
        balance = "0",
        totalReceived = null,
        totalSent = null,
        unconfirmedBalance = null,
        unconfirmedTxs = null,
        txs = txs,
        usedTokens = null,
        transactions = transactions,
        tokens = null,
    )

    private fun tx(
        txid: String,
        fees: String,
        vin: List<GetAddressResponse.Transaction.Vin>,
        vout: List<GetAddressResponse.Transaction.Vout>,
        confirmations: Int = 10,
        blockTime: Int = 1_700_000_000,
    ) = GetAddressResponse.Transaction(
        txid = txid,
        vout = vout,
        confirmations = confirmations,
        blockTime = blockTime,
        value = "0",
        vin = vin,
        fees = fees,
        tokenTransfers = emptyList(),
        ethereumSpecific = null,
        chainExtraData = null,
        tronTXReceipt = null,
        fromAddress = null,
        toAddress = null,
        contractType = null,
        contractName = null,
        voteList = null,
    )

    private fun vin(address: String, value: String, isOwn: Boolean?) =
        GetAddressResponse.Transaction.Vin(addresses = listOf(address), value = value, isOwn = isOwn)

    private fun vout(address: String, value: String, isOwn: Boolean?) =
        GetAddressResponse.Transaction.Vout(addresses = listOf(address), hex = null, value = value, isOwn = isOwn)

    private companion object {
        const val DESCRIPTOR = "pkh(xpub6BwskqRoNd523sEbE7h6hGGg2qT6zyJ5pQKeXESCTSe5iiYcPUmtMQJTL3nwnkgyAq)"
        const val OWN_MAIN = "DHqHWttTZAeCy5iG2ci11dBvF1ripsu2n8"
        const val OWN_RECEIVE = "DHhRz1pe88eAFudkfUkm33RhhZBF8Aq5zq"
        const val SENDER = "DEyPToL2QvQ53xVYQABdHxPtSFKs8po3DS"
        const val SENDER_CHANGE = "DCJAEryGhD6N6LM4zLy8mqxtuZ7TUXWDNN"
        const val RECIPIENT = "DB9GuKRqJh9v8sjCNzpCCXEcfxv9PjJqW1"
        const val RECIPIENT2 = "DK9Ah3ZTs44bXzGdUBQLN5nsXhFeoqJGvM"
        const val PAGE_SIZE = 20
    }

    // endregion
}