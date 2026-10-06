package com.tangem.blockchain.transactionhistory.blockchains.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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

    @Test
    fun `several incoming transfers of one transaction are merged into a single item with the total amount`() =
        runTest {
            // A swap payout arrives as two USDT transfers from the same settlement contract:
            // 9.534256 + 0.345802 = 9.880058
            val tx = transaction(
                tokenTransfers = listOf(
                    tokenTransfer(from = SETTLEMENT, to = WALLET, value = "9534256"),
                    tokenTransfer(from = SETTLEMENT, to = WALLET, value = "345802"),
                ),
            )

            val item = tokenItems(tx).single()

            assertThat(item.amount.value!!.compareTo(BigDecimal("9.880058"))).isEqualTo(0)
            assertThat(item.isOutgoing).isFalse()
            assertThat(item.txHash).isEqualTo(TX_HASH)
            assertThat((item.sourceType as TransactionHistoryItem.SourceType.Single).address).isEqualTo(SETTLEMENT)
            val destination = item.destinationType as TransactionHistoryItem.DestinationType.Single
            assertThat(destination.addressType.address).isEqualTo(WALLET)
        }

    @Test
    fun `transfers to different counterparties stay separate rows`() = runTest {
        // The gasless flow pays the recipient and the fee service within one transaction, and the fee is recognised
        // downstream by its destination address, so the two charges must not collapse into one row.
        val tx = transaction(
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                tokenTransfer(from = WALLET, to = FEE_COLLECTOR, value = "63146"),
            ),
        )

        val items = tokenItems(tx)

        assertThat(items).hasSize(2)
        assertThat(
            items.map { (it.destinationType as TransactionHistoryItem.DestinationType.Single).addressType.address },
        )
            .containsExactly(RECIPIENT, FEE_COLLECTOR)
            .inOrder()
        assertThat(items[0].amount.value!!.compareTo(BigDecimal("3"))).isEqualTo(0)
        assertThat(items[1].amount.value!!.compareTo(BigDecimal("0.063146"))).isEqualTo(0)
    }

    @Test
    fun `incoming and outgoing transfers of the same counterparty stay separate rows`() = runTest {
        val tx = transaction(
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = SETTLEMENT, value = "5000000"),
                tokenTransfer(from = SETTLEMENT, to = WALLET, value = "1000000"),
            ),
        )

        val items = tokenItems(tx)

        assertThat(items).hasSize(2)
        assertThat(items.map { it.isOutgoing }).containsExactly(true, false).inOrder()
    }

    @Test
    fun `counterparty address casing does not split a single transfer group`() = runTest {
        val tx = transaction(
            tokenTransfers = listOf(
                tokenTransfer(from = SETTLEMENT, to = WALLET, value = "1000000"),
                tokenTransfer(from = SETTLEMENT.uppercase(), to = WALLET, value = "2000000"),
            ),
        )

        val item = tokenItems(tx).single()

        assertThat(item.amount.value!!.compareTo(BigDecimal("3"))).isEqualTo(0)
    }

    @Test
    fun `transfers of another token are ignored`() = runTest {
        val tx = transaction(
            tokenTransfers = listOf(
                tokenTransfer(from = SETTLEMENT, to = WALLET, value = "1000000"),
                tokenTransfer(from = SETTLEMENT, to = WALLET, value = "7000000", contract = OTHER_CONTRACT),
            ),
        )

        val item = tokenItems(tx).single()

        assertThat(item.amount.value!!.compareTo(BigDecimal.ONE)).isEqualTo(0)
    }

    @Test
    fun `self transfers are ignored`() = runTest {
        val tx = transaction(
            tokenTransfers = listOf(tokenTransfer(from = WALLET, to = WALLET, value = "1000000")),
        )

        assertThat(tokenItems(tx)).isEmpty()
    }

    @Test
    fun `group of zero-value transfers is excluded from history`() = runTest {
        val tx = transaction(
            tokenTransfers = listOf(
                tokenTransfer(from = SETTLEMENT, to = WALLET, value = "0"),
                tokenTransfer(from = SETTLEMENT, to = WALLET, value = null),
            ),
        )

        assertThat(tokenItems(tx)).isEmpty()
    }

    @Test
    fun `fee paid in another token replaces the native fee of a gaslessly sent transfer`() = runTest {
        // The relayer paid the ETH gas; the wallet paid the service 0.065869 USDC, and that is what the transfer
        // cost its owner
        val tx = transaction(
            sender = RELAYER,
            recipient = GASLESS_ENTRY_POINT,
            data = GASLESS_CALL_DATA,
            fees = "19190194523865",
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                feeTransfer(),
            ),
        )

        val item = tokenItems(tx, gaslessFeeAddresses = setOf(FEE_COLLECTOR)).single()

        assertThat(item.amount.value!!.compareTo(BigDecimal("3"))).isEqualTo(0)
        assertThat(item.fee.currencySymbol).isEqualTo("USDC")
        assertThat(item.fee.value!!.compareTo(BigDecimal("0.065869"))).isEqualTo(0)
    }

    @Test
    fun `fee collector address casing does not hide the fee paid in another token`() = runTest {
        val tx = transaction(
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                feeTransfer(to = FEE_COLLECTOR.uppercase()),
            ),
        )

        val item = tokenItems(tx, gaslessFeeAddresses = setOf(FEE_COLLECTOR)).single()

        assertThat(item.fee.currencySymbol).isEqualTo("USDC")
    }

    @Test
    fun `transfer of another token to an unknown address is not taken for the gasless fee`() = runTest {
        val tx = transaction(
            fees = "19190194523865",
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                feeTransfer(to = SETTLEMENT),
            ),
        )

        val item = tokenItems(tx, gaslessFeeAddresses = setOf(FEE_COLLECTOR)).single()

        assertThat(item.fee.currencySymbol).isEqualTo(Blockchain.Ethereum.currency)
        assertThat(item.fee.value!!.compareTo(BigDecimal("0.000019190194523865"))).isEqualTo(0)
    }

    @Test
    fun `fee paid in the viewed token stays a row of its own and leaves the native fee alone`() = runTest {
        val tx = transaction(
            fees = "19190194523865",
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                tokenTransfer(from = WALLET, to = FEE_COLLECTOR, value = "65869"),
            ),
        )

        val items = tokenItems(tx, gaslessFeeAddresses = setOf(FEE_COLLECTOR))

        assertThat(items).hasSize(2)
        assertThat(items.map { it.fee.currencySymbol }).containsExactly(
            Blockchain.Ethereum.currency,
            Blockchain.Ethereum.currency,
        )
    }

    @Test
    fun `fee paid in another token is left alone when the caller knows no fee collector`() = runTest {
        val tx = transaction(
            fees = "19190194523865",
            tokenTransfers = listOf(
                tokenTransfer(from = WALLET, to = RECIPIENT, value = "3000000"),
                feeTransfer(),
            ),
        )

        val item = tokenItems(tx).single()

        assertThat(item.fee.currencySymbol).isEqualTo(Blockchain.Ethereum.currency)
    }

    // region helpers

    @Test
    fun `cancelled history request propagates the cancellation instead of a failure result`() = runTest {
        val filterType = TransactionHistoryRequest.FilterType.Coin
        val request = TransactionHistoryRequest(
            address = WALLET,
            decimals = Blockchain.Ethereum.decimals(),
            page = Page.Initial,
            pageSize = PAGE_SIZE,
            filterType = filterType,
        )
        coEvery { blockBookApi.getTransactions(WALLET, null, PAGE_SIZE, filterType) } throws CancellationException()

        val error = try {
            provider.getTransactionsHistory(request)
            null
        } catch (e: CancellationException) {
            e
        }

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `internal calls of a Safe execution become outgoing transfers of the Safe`() = runTest {
        // The relayer's execTransaction moves no ETH itself: the transfer and the refund are internal calls
        val tx = transaction(
            sender = SAFE_RELAYER,
            recipient = SAFE,
            internalTransfers = listOf(
                internalCall(from = SAFE, to = SAFE_RECIPIENT, value = "159680614810478953"),
                internalCall(from = SAFE, to = SAFE_RELAYER, value = "33203274238597392"),
            ),
        )

        val internal = safeCoinItems(tx).filter { it.type == TransactionHistoryItem.TransactionType.Transfer }

        assertThat(internal).containsExactly(
            safeTransfer(to = SAFE_RECIPIENT, wei = "159680614810478953"),
            safeTransfer(to = SAFE_RELAYER, wei = "33203274238597392"),
        ).inOrder()
    }

    @Test
    fun `deploy bundle where the Safe is neither sender nor recipient still yields its internal calls`() = runTest {
        val tx = transaction(
            sender = WALLET,
            recipient = MULTICALL3,
            internalTransfers = listOf(
                internalCall(from = PROXY_FACTORY, to = SAFE, value = "0", type = 1),
                internalCall(from = SAFE, to = SAFE_RECIPIENT, value = "1093522368049869555"),
                internalCall(from = SAFE, to = SAFE_RELAYER, value = "129211841622438168"),
            ),
        )

        assertThat(safeCoinItems(tx)).containsExactly(
            safeTransfer(to = SAFE_RECIPIENT, wei = "1093522368049869555"),
            safeTransfer(to = SAFE_RELAYER, wei = "129211841622438168"),
        ).inOrder()
    }

    @Test
    fun `zero-value and foreign internal calls are not history items`() = runTest {
        val tx = transaction(
            sender = WALLET,
            recipient = MULTICALL3,
            internalTransfers = listOf(
                internalCall(from = SAFE, to = SAFE_RECIPIENT, value = "0"),
                internalCall(from = SETTLEMENT, to = RECIPIENT, value = "1000"),
            ),
        )

        assertThat(safeCoinItems(tx)).isEmpty()
    }

    @Test
    fun `internal calls of a transaction with an execution error are failed`() = runTest {
        val tx = transaction(
            sender = WALLET,
            recipient = MULTICALL3,
            error = "execution reverted",
            internalTransfers = listOf(internalCall(from = SAFE, to = SAFE_RECIPIENT, value = "462356000000000000")),
        )

        assertThat(safeCoinItems(tx)).containsExactly(
            safeTransfer(
                to = SAFE_RECIPIENT,
                wei = "462356000000000000",
                status = TransactionHistoryItem.TransactionStatus.Failed,
            ),
        )
    }

    @Test
    fun `without the flag internal calls are ignored and the coin filter is kept`() = runTest {
        val tx = transaction(
            sender = WALLET,
            recipient = MULTICALL3,
            internalTransfers = listOf(internalCall(from = WALLET, to = RECIPIENT, value = "1000")),
        )

        assertThat(coinItems(tx)).isEmpty()
    }

    @Test
    fun `cancelled history state request propagates the cancellation instead of a failed state`() = runTest {
        coEvery {
            blockBookApi.getTransactions(WALLET, null, 1, TransactionHistoryRequest.FilterType.Coin)
        } throws CancellationException()

        val error = try {
            provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)
            null
        } catch (e: CancellationException) {
            e
        }

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

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

    private suspend fun tokenItems(
        tx: GetAddressResponse.Transaction,
        gaslessFeeAddresses: Set<String> = emptySet(),
    ): List<TransactionHistoryItem> {
        val filterType = TransactionHistoryRequest.FilterType.Contract(tokenInfo = TOKEN)
        val request = TransactionHistoryRequest(
            address = WALLET,
            decimals = TOKEN.decimals,
            page = Page.Initial,
            pageSize = PAGE_SIZE,
            filterType = filterType,
            gaslessFeeAddresses = gaslessFeeAddresses,
        )
        coEvery {
            blockBookApi.getTransactions(WALLET, null, PAGE_SIZE, filterType)
        } returns addressResponse(transactions = listOf(tx))

        return (provider.getTransactionsHistory(request) as Result.Success).data.items
    }

    private suspend fun safeCoinItems(tx: GetAddressResponse.Transaction): List<TransactionHistoryItem> {
        val request = TransactionHistoryRequest(
            address = SAFE,
            decimals = Blockchain.Ethereum.decimals(),
            page = Page.Initial,
            pageSize = PAGE_SIZE,
            filterType = TransactionHistoryRequest.FilterType.Coin,
            shouldIncludeInternalTransfers = true,
        )
        coEvery { blockBookApi.getTransactions(SAFE, null, PAGE_SIZE, null) } returns
            addressResponse(transactions = listOf(tx))

        return (provider.getTransactionsHistory(request) as Result.Success).data.items
    }

    private fun internalCall(from: String, to: String, value: String, type: Int = 0) =
        GetAddressResponse.Transaction.EthereumSpecific.InternalTransfer(
            type = type,
            from = from,
            to = to,
            value = value,
        )

    private fun safeTransfer(
        to: String,
        wei: String,
        status: TransactionHistoryItem.TransactionStatus = TransactionHistoryItem.TransactionStatus.Confirmed,
    ) = TransactionHistoryItem(
        txHash = TX_HASH,
        timestamp = 1_754_563_859_000L,
        isOutgoing = true,
        destinationType = TransactionHistoryItem.DestinationType.Single(TransactionHistoryItem.AddressType.User(to)),
        sourceType = TransactionHistoryItem.SourceType.Single(SAFE),
        status = status,
        type = TransactionHistoryItem.TransactionType.Transfer,
        amount = Amount(
            value = BigDecimal(wei).movePointLeft(Blockchain.Ethereum.decimals()),
            blockchain = Blockchain.Ethereum,
            type = AmountType.Coin,
        ),
        fee = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
    )

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
        fees: String = "0",
        internalTransfers: List<GetAddressResponse.Transaction.EthereumSpecific.InternalTransfer>? = null,
        error: String? = null,
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
        fees = fees,
        tokenTransfers = tokenTransfers,
        ethereumSpecific = GetAddressResponse.Transaction.EthereumSpecific(
            status = GetAddressResponse.Transaction.StatusType.OK,
            nonce = null,
            gasLimit = null,
            gasUsed = null,
            gasPrice = null,
            data = data,
            parsedData = null,
            error = error,
            internalTransfers = internalTransfers,
        ),
        chainExtraData = null,
        tronTXReceipt = null,
        fromAddress = null,
        toAddress = null,
        contractType = null,
        contractName = null,
        voteList = null,
    )

    private fun tokenTransfer(
        from: String,
        to: String,
        value: String?,
        contract: String = CONTRACT,
        symbol: String = "USDT",
        name: String = "Tether USD",
        decimals: Int = 6,
    ) = GetAddressResponse.Transaction.TokenTransfer(
        type = "ERC20",
        from = from,
        to = to,
        contract = contract,
        token = contract,
        name = name,
        symbol = symbol,
        decimals = decimals,
        value = value,
    )

    private fun feeTransfer(to: String = FEE_COLLECTOR) = tokenTransfer(
        from = WALLET,
        to = to,
        value = "65869",
        contract = OTHER_CONTRACT,
        symbol = "USDC",
        name = "USD Coin",
    )

    private companion object {
        const val WALLET = "0x4e272d36498f2dd90016d0336304fc985f4dd626"
        const val SAFE = "0xaf51086ffe17d431d0b046a66bf5a4888fdb4dcc"
        const val SAFE_RECIPIENT = "0x35995d88a44ee79ddcd03478deefbbba815b0769"
        const val SAFE_RELAYER = "0xef329c418c43e4afba82a4981d3447207bc2d8ae"
        const val MULTICALL3 = "0xca11bde05977b3631167028862be2a173976ca11"
        const val PROXY_FACTORY = "0x4e1dcf7ad4e460cfd30791ccc4f9c8a4f820ec67"
        const val SETTLEMENT = "0x18dc29ca1b75c9a07d4d8f82b5676a0b2b4725b4"
        const val RECIPIENT = "0x1b6257cae4192e62b629efca21771be3d759183d"
        const val FEE_COLLECTOR = "0x0eb8d76a3895938870f70aa77a58214f27ef9aa9"
        const val RELAYER = "0x05917c2b4811e78deaa4f68f3900242acfb9beab"
        const val GASLESS_ENTRY_POINT = "0x9a74442ad2d0c8c2ca035a6f9b6122a085e72f0f"
        const val CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7"
        const val OTHER_CONTRACT = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val TX_HASH = "0x37d5db34b233fded3f0a3a044cc49257c61f345c43f3e748c990e0141f3996fa"
        const val DELEGATION_TX_HASH = "0x3cc0e0f3be221d0c3ff2c4671846603d17d5618a7158e89a85190a8b6d998ebb"
        const val PAGE_SIZE = 20

        /** Method id the gasless relayer calls in the reported EIP-7702 transaction. */
        const val GASLESS_CALL_DATA = "0x6234d42b"

        /** `approve(address,uint256)`. */
        const val APPROVE_CALL_DATA = "0x095ea7b3"

        val TOKEN = Token(
            name = "Tether USD",
            symbol = "USDT",
            contractAddress = CONTRACT,
            decimals = 6,
        )
    }

    // endregion
}