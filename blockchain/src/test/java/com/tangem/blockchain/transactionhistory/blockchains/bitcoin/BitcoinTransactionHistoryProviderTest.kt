package com.tangem.blockchain.transactionhistory.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
 * Tests for the single-address transaction-history path of [BitcoinTransactionHistoryProvider].
 *
 * For a bare multisig script BlockBook lists the P2PKH address of every key in it with `isAddress = false`,
 * so a script that only includes the wallet's key must not be attributed to the wallet.
 */
class BitcoinTransactionHistoryProviderTest {

    private val blockBookApi = mockk<BlockBookApi>()
    private val provider = BitcoinTransactionHistoryProvider(
        blockchain = Blockchain.Dogecoin,
        blockBookApi = blockBookApi,
    )

    @Test
    fun `regular send is outgoing with external outputs plus fee`() = runTest {
        val tx = tx(
            fees = "1000000",
            vin = listOf(vin(listOf(WALLET), "500000000")),
            vout = listOf(
                vout(listOf(RECIPIENT), "100000000"),
                vout(listOf(WALLET), "399000000"),
            ),
        )

        val item = singleItem(tx)

        assertThat(item.isOutgoing).isTrue()
        // sent = 100000000 + fee 1000000 = 1.01 DOGE
        assertThat(item.amount.value!!.compareTo(BigDecimal("1.01"))).isEqualTo(0)
        assertThat(item.sourceType).isEqualTo(TransactionHistoryItem.SourceType.Single(WALLET))
        assertThat(item.destinationType).isEqualTo(
            TransactionHistoryItem.DestinationType.Single(TransactionHistoryItem.AddressType.User(RECIPIENT)),
        )
    }

    @Test
    fun `missing isAddress falls back to the exact address match`() = runTest {
        val tx = tx(
            fees = "1000000",
            vin = listOf(vin(listOf(WALLET), "500000000", isAddress = null)),
            vout = listOf(
                vout(listOf(RECIPIENT), "100000000", isAddress = null),
                vout(listOf(WALLET), "399000000", isAddress = null),
            ),
        )

        val item = singleItem(tx)

        assertThat(item.isOutgoing).isTrue()
        assertThat(item.amount.value!!.compareTo(BigDecimal("1.01"))).isEqualTo(0)
    }

    @Test
    fun `spending a bare multisig with the wallet key is not outgoing`() = runTest {
        // Bug bounty PoC: 1-of-2 [attacker, wallet] spent by the attacker's key alone,
        // 1499 DOGE to a look-alike address and 0.01 DOGE dust to the wallet to surface in its history
        val tx = tx(
            fees = "1000000",
            vin = listOf(vin(listOf(ATTACKER, WALLET), "149902000000", isAddress = false)),
            vout = listOf(
                vout(listOf(LOOKALIKE), "149900000000"),
                vout(listOf(WALLET), "1000000"),
            ),
        )

        val item = singleItem(tx)

        assertThat(item.isOutgoing).isFalse()
        assertThat(item.amount.value!!.compareTo(BigDecimal("0.01"))).isEqualTo(0)
        assertThat(item.sourceType).isEqualTo(TransactionHistoryItem.SourceType.Multiple(listOf(ATTACKER, WALLET)))
    }

    @Test
    fun `spending a zero-of-one multisig with only the wallet key is not outgoing`() = runTest {
        // OP_0 <wallet key> OP_1 OP_CHECKMULTISIG needs no signature, yet BlockBook lists only the wallet's address
        val tx = tx(
            fees = "1000000",
            vin = listOf(vin(listOf(WALLET), "149902000000", isAddress = false)),
            vout = listOf(
                vout(listOf(LOOKALIKE), "149900000000"),
                vout(listOf(WALLET), "1000000"),
            ),
        )

        val item = singleItem(tx)

        assertThat(item.isOutgoing).isFalse()
        assertThat(item.amount.value!!.compareTo(BigDecimal("0.01"))).isEqualTo(0)
    }

    @Test
    fun `incoming bare multisig output with the wallet key is not counted as received`() = runTest {
        val tx = tx(
            fees = "1000000",
            vin = listOf(vin(listOf(ATTACKER), "1000000000")),
            vout = listOf(
                vout(listOf(ATTACKER, WALLET), "998900000", isAddress = false),
                vout(listOf(WALLET), "100000"),
            ),
        )

        val item = singleItem(tx)

        assertThat(item.isOutgoing).isFalse()
        assertThat(item.amount.value!!.compareTo(BigDecimal("0.001"))).isEqualTo(0)
    }

    @Test
    fun `outgoing bare multisig output with the wallet key is counted as sent not as change`() = runTest {
        val tx = tx(
            fees = "1000000",
            vin = listOf(vin(listOf(WALLET), "500000000")),
            vout = listOf(
                vout(listOf(RECIPIENT, WALLET), "100000000", isAddress = false),
                vout(listOf(WALLET), "399000000"),
            ),
        )

        val item = singleItem(tx)

        assertThat(item.isOutgoing).isTrue()
        assertThat(item.amount.value!!.compareTo(BigDecimal("1.01"))).isEqualTo(0)
    }

    private suspend fun singleItem(tx: GetAddressResponse.Transaction): TransactionHistoryItem {
        coEvery { blockBookApi.getTransactions(WALLET, null, PAGE_SIZE, null) } returns addressResponse(listOf(tx))
        val result = provider.getTransactionsHistory(
            TransactionHistoryRequest(
                address = WALLET,
                decimals = Blockchain.Dogecoin.decimals(),
                page = Page.Initial,
                pageSize = PAGE_SIZE,
                filterType = TransactionHistoryRequest.FilterType.Coin,
            ),
        )
        return (result as Result.Success).data.items.single()
    }

    private fun addressResponse(transactions: List<GetAddressResponse.Transaction>) = GetAddressResponse(
        balance = "0",
        unconfirmedTxs = null,
        txs = transactions.size,
        transactions = transactions,
        page = 1,
        totalPages = 1,
        itemsOnPage = null,
        trxTokens = null,
    )

    private fun tx(
        fees: String,
        vin: List<GetAddressResponse.Transaction.Vin>,
        vout: List<GetAddressResponse.Transaction.Vout>,
    ) = GetAddressResponse.Transaction(
        txid = "txid",
        vout = vout,
        confirmations = 10,
        blockTime = 1_700_000_000,
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

    private fun vin(addresses: List<String>, value: String, isAddress: Boolean? = true) =
        GetAddressResponse.Transaction.Vin(addresses = addresses, value = value, isAddress = isAddress)

    private fun vout(addresses: List<String>, value: String, isAddress: Boolean? = true) =
        GetAddressResponse.Transaction.Vout(addresses = addresses, hex = null, value = value, isAddress = isAddress)

    private companion object {
        // Bug bounty PoC addresses: P2PKH of the secp256k1 generator G (wallet) and of 2G (attacker)
        const val WALLET = "DFpN6QqFfUm3gKNaxN6tNcab1FArL9cZLE"
        const val ATTACKER = "D5kTEGxmas71USKAcAaJptKjq8NJc6EK8C"
        const val LOOKALIKE = "DBHKtAHTwf1nQWDKcX3SnY6L1G4puqadJf"
        const val RECIPIENT = "DB9GuKRqJh9v8sjCNzpCCXEcfxv9PjJqW1"
        const val PAGE_SIZE = 20
    }
}