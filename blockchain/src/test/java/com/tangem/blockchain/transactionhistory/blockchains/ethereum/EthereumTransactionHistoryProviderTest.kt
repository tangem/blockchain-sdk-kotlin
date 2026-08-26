package com.tangem.blockchain.transactionhistory.blockchains.ethereum

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

class EthereumTransactionHistoryProviderTest {

    private val blockBookApi = mockk<BlockBookApi>()
    private val provider = EthereumTransactionHistoryProvider(
        blockchain = Blockchain.Ethereum,
        blockBookApi = blockBookApi,
    )

    @Test
    fun `transaction of an unrelated sender and recipient is excluded from coin history`() = runTest {
        // The reported transaction, as the node reports it: an EIP-7702 transfer submitted by the gasless relayer.
        // The wallet is only named in the authorization list and in the token transfers, never as sender or
        // recipient, and the transaction carries no ETH — so the coin history must not show it
        val tx = transaction(
            txHash = DELEGATION_TX_HASH,
            sender = RELAYER,
            recipient = GASLESS_ENTRY_POINT,
            data = GASLESS_CALL_DATA,
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                tokenTransfer(from = WALLET, to = FEE_COLLECTOR, value = "63146"),
            ),
        )

        assertThat(coinItems(tx)).isEmpty()
    }

    @Test
    fun `outgoing coin transfer is kept in coin history`() = runTest {
        val tx = transaction(sender = WALLET, recipient = RECIPIENT, value = "1000000000000000000")

        val item = coinItems(tx).single()

        assertThat(item.isOutgoing).isTrue()
        assertThat(item.amount.value!!.compareTo(BigDecimal.ONE)).isEqualTo(0)
    }

    @Test
    fun `incoming coin transfer is kept in coin history`() = runTest {
        val tx = transaction(sender = SETTLEMENT, recipient = WALLET, value = "1000000000000000000")

        val item = coinItems(tx).single()

        assertThat(item.isOutgoing).isFalse()
        assertThat(item.amount.value!!.compareTo(BigDecimal.ONE)).isEqualTo(0)
    }

    @Test
    fun `zero-value contract call signed by the wallet is kept in coin history`() = runTest {
        // An approval carries no ETH but is a user action, so it stays visible
        val tx = transaction(sender = WALLET, recipient = CONTRACT, value = "0", data = APPROVE_CALL_DATA)

        val item = coinItems(tx).single()

        assertThat(item.isOutgoing).isTrue()
        assertThat(item.type).isInstanceOf(TransactionHistoryItem.TransactionType.ContractMethod::class.java)
    }

    @Test
    fun `sender address casing does not hide an outgoing coin transfer`() = runTest {
        val tx = transaction(sender = WALLET.uppercase(), recipient = RECIPIENT, value = "1000000000000000000")

        assertThat(coinItems(tx).single().isOutgoing).isTrue()
    }

    @Test
    fun `transaction without a sender is excluded from coin history`() = runTest {
        val tx = transaction(sender = null, recipient = WALLET, value = "1000000000000000000")

        assertThat(coinItems(tx)).isEmpty()
    }

    // region helpers

    private suspend fun coinItems(tx: GetAddressResponse.Transaction): List<TransactionHistoryItem> {
        val filterType = TransactionHistoryRequest.FilterType.Coin
        val request = TransactionHistoryRequest(
            address = WALLET,
            decimals = Blockchain.Ethereum.decimals(),
            page = Page.Initial,
            pageSize = PAGE_SIZE,
            filterType = filterType,
        )
        coEvery {
            blockBookApi.getTransactions(WALLET, null, PAGE_SIZE, filterType)
        } returns addressResponse(transactions = listOf(tx))

        return (provider.getTransactionsHistory(request) as Result.Success).data.items
    }

    private fun addressResponse(transactions: List<GetAddressResponse.Transaction>?) = GetAddressResponse(
        balance = "0",
        unconfirmedTxs = 0,
        txs = transactions?.size ?: 0,
        transactions = transactions,
        page = 1,
        totalPages = 1,
        itemsOnPage = null,
        trxTokens = null,
    )

    private fun transaction(
        tokenTransfers: List<GetAddressResponse.Transaction.TokenTransfer> = emptyList(),
        sender: String? = WALLET,
        recipient: String? = SETTLEMENT,
        value: String = "0",
        data: String? = null,
        txHash: String = TX_HASH,
    ) = GetAddressResponse.Transaction(
        txid = txHash,
        vout = listOfNotNull(
            recipient?.let {
                GetAddressResponse.Transaction.Vout(addresses = listOf(it), hex = null, value = value)
            },
        ),
        confirmations = 12,
        blockTime = 1_754_563_859,
        value = value,
        vin = listOfNotNull(
            sender?.let { GetAddressResponse.Transaction.Vin(addresses = listOf(it), value = value) },
        ),
        fees = "0",
        tokenTransfers = tokenTransfers,
        ethereumSpecific = GetAddressResponse.Transaction.EthereumSpecific(
            status = GetAddressResponse.Transaction.StatusType.OK,
            nonce = null,
            gasLimit = null,
            gasUsed = null,
            gasPrice = null,
            data = data,
            parsedData = null,
        ),
        chainExtraData = null,
        tronTXReceipt = null,
        fromAddress = null,
        toAddress = null,
        contractType = null,
        contractName = null,
        voteList = null,
    )

    private fun tokenTransfer(from: String, to: String, value: String?, contract: String = CONTRACT) =
        GetAddressResponse.Transaction.TokenTransfer(
            type = "ERC20",
            from = from,
            to = to,
            contract = contract,
            token = contract,
            name = "Tether USD",
            symbol = "USDT",
            decimals = 6,
            value = value,
        )

    private companion object {
        const val WALLET = "0x4e272d36498f2dd90016d0336304fc985f4dd626"
        const val SETTLEMENT = "0x18dc29ca1b75c9a07d4d8f82b5676a0b2b4725b4"
        const val RECIPIENT = "0x1b6257cae4192e62b629efca21771be3d759183d"
        const val FEE_COLLECTOR = "0x0eb8d76a3895938870f70aa77a58214f27ef9aa9"
        const val RELAYER = "0x05917c2b4811e78deaa4f68f3900242acfb9beab"
        const val GASLESS_ENTRY_POINT = "0x9a74442ad2d0c8c2ca035a6f9b6122a085e72f0f"
        const val CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7"
        const val TX_HASH = "0x37d5db34b233fded3f0a3a044cc49257c61f345c43f3e748c990e0141f3996fa"
        const val DELEGATION_TX_HASH = "0x3cc0e0f3be221d0c3ff2c4671846603d17d5618a7158e89a85190a8b6d998ebb"
        const val PAGE_SIZE = 20

        /** Method id the gasless relayer calls in the reported EIP-7702 transaction. */
        const val GASLESS_CALL_DATA = "0x6234d42b"

        /** `approve(address,uint256)`. */
        const val APPROVE_CALL_DATA = "0x095ea7b3"
    }

    // endregion
}