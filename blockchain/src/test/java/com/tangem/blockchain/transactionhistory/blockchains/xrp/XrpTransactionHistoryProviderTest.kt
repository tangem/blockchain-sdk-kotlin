package com.tangem.blockchain.transactionhistory.blockchains.xrp

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.xrp.network.XrpAccountTxRequest
import com.tangem.blockchain.blockchains.xrp.network.XrpAccountTxResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpIssuedCurrencyAmount
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTransaction
import com.tangem.blockchain.blockchains.xrp.network.XrpTransactionAmount
import com.tangem.blockchain.blockchains.xrp.network.XrpTransactionMarker
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

class XrpTransactionHistoryProviderTest {

    private val networkProvider = mockk<XrpNetworkProvider>()
    private val provider = XrpTransactionHistoryProvider(
        blockchain = Blockchain.XRP,
        networkProvider = networkProvider,
    )

    // region getTransactionHistoryState

    @Test
    fun `state returns HasTransactions when transactions present`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(payment())

        val state = provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.HasTransactions(txCount = 1))
    }

    @Test
    fun `state requests a single transaction only`() = runTest {
        val requests = mutableListOf<XrpAccountTxRequest>()
        coEvery { networkProvider.getAccountTransactions(capture(requests)) } returns success()

        provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(requests.single()).isEqualTo(XrpAccountTxRequest(address = WALLET, limit = 1, marker = null))
    }

    @Test
    fun `state returns Empty when there are no transactions`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success()

        val state = provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isEqualTo(TransactionHistoryState.Success.Empty)
    }

    @Test
    fun `state returns FetchError when request fails`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns
            Result.Failure(BlockchainSdkError.CustomError("failed"))

        val state = provider.getTransactionHistoryState(WALLET, TransactionHistoryRequest.FilterType.Coin)

        assertThat(state).isInstanceOf(TransactionHistoryState.Failed.FetchError::class.java)
    }

    // endregion

    // region coin history

    @Test
    fun `outgoing coin payment is mapped`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(account = WALLET, destination = COUNTERPARTY),
        )

        val items = provider.coinHistory()

        assertThat(items).containsExactly(
            TransactionHistoryItem(
                txHash = HASH,
                timestamp = 1_755_609_600_000L,
                isOutgoing = true,
                destinationType = TransactionHistoryItem.DestinationType.Single(
                    addressType = TransactionHistoryItem.AddressType.User(COUNTERPARTY),
                ),
                sourceType = TransactionHistoryItem.SourceType.Single(address = WALLET),
                status = TransactionHistoryItem.TransactionStatus.Confirmed,
                type = TransactionHistoryItem.TransactionType.Transfer,
                amount = Amount(value = BigDecimal("1.500000"), blockchain = Blockchain.XRP, type = AmountType.Coin),
                fee = Amount(blockchain = Blockchain.XRP, value = BigDecimal("0.000012")),
            ),
        )
    }

    @Test
    fun `incoming coin payment is not outgoing`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(account = COUNTERPARTY, destination = WALLET),
        )

        assertThat(provider.coinHistory().single().isOutgoing).isFalse()
    }

    @Test
    fun `issued currency payment is skipped in coin history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(amount = XrpTransactionAmount.IssuedCurrency(issuedAmount(BigDecimal.ONE))),
        )

        assertThat(provider.coinHistory()).isEmpty()
    }

    @Test
    fun `trust set is skipped in coin history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(trustSet())

        assertThat(provider.coinHistory()).isEmpty()
    }

    @Test
    fun `transaction without fee is skipped`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(payment(feeInDrops = null))

        assertThat(provider.coinHistory()).isEmpty()
    }

    @Test
    fun `zero amount transaction is excluded`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(amount = XrpTransactionAmount.Drops(BigDecimal.ZERO)),
        )

        assertThat(provider.coinHistory()).isEmpty()
    }

    // endregion

    // region status and type

    @Test
    fun `unsuccessful engine result maps to Failed`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(transactionResult = "tecUNFUNDED_PAYMENT"),
        )

        assertThat(provider.coinHistory().single().status)
            .isEqualTo(TransactionHistoryItem.TransactionStatus.Failed)
    }

    @Test
    fun `not validated transaction maps to Unconfirmed`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(payment(isValidated = false))

        assertThat(provider.coinHistory().single().status)
            .isEqualTo(TransactionHistoryItem.TransactionStatus.Unconfirmed)
    }

    @Test
    fun `non payment type is mapped to contract method name`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            trustSet().copy(transactionType = "EscrowFinish", amount = XrpTransactionAmount.Drops(DROPS)),
        )

        assertThat(provider.coinHistory().single().type)
            .isEqualTo(TransactionHistoryItem.TransactionType.ContractMethodName(name = "EscrowFinish"))
    }

    // endregion

    // region token history

    @Test
    fun `matching token payment is mapped with denominated amount`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(amount = XrpTransactionAmount.IssuedCurrency(issuedAmount(BigDecimal("12.34")))),
        )

        val item = provider.tokenHistory().single()

        assertThat(item.amount).isEqualTo(Amount(value = BigDecimal("12.34"), token = TOKEN))
        assertThat(item.fee).isEqualTo(Amount(blockchain = Blockchain.XRP, value = BigDecimal("0.000012")))
    }

    @Test
    fun `token contract address with dash separator is matched`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(amount = XrpTransactionAmount.IssuedCurrency(issuedAmount(BigDecimal.ONE))),
        )

        val token = TOKEN.copy(contractAddress = "$CURRENCY-$ISSUER")

        assertThat(provider.tokenHistory(token)).hasSize(1)
    }

    @Test
    fun `payment of another issuer is skipped`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            payment(
                amount = XrpTransactionAmount.IssuedCurrency(
                    issuedAmount(value = BigDecimal.ONE, issuer = "rOtherIssuer"),
                ),
            ),
        )

        assertThat(provider.tokenHistory()).isEmpty()
    }

    @Test
    fun `coin payment is skipped in token history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(payment())

        assertThat(provider.tokenHistory()).isEmpty()
    }

    @Test
    fun `trust set is matched by limit amount but reports no transferred amount`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(trustSet())

        val item = provider.tokenHistory().single()

        assertThat(item.amount).isEqualTo(Amount(value = BigDecimal.ZERO, token = TOKEN))
        assertThat(item.destinationType).isEqualTo(
            TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(ISSUER),
            ),
        )
        assertThat(item.type).isEqualTo(TransactionHistoryItem.TransactionType.ContractMethodName(name = "TrustSet"))
    }

    @Test
    fun `trust set with the maximum limit reports no transferred amount`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            trustSet(limitAmount = issuedAmount(BigDecimal("9999999999999999e80"))),
        )

        assertThat(provider.tokenHistory().single().amount)
            .isEqualTo(Amount(value = BigDecimal.ZERO, token = TOKEN))
    }

    @Test
    fun `trust set of another issuer is skipped`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            trustSet(limitAmount = issuedAmount(value = BigDecimal.ONE, issuer = "rOtherIssuer")),
        )

        assertThat(provider.tokenHistory()).isEmpty()
    }

    // endregion

    // region supported transaction types

    @Test
    fun `unsupported transaction type is skipped`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation("AccountSet", amount = XrpTransactionAmount.Drops(DROPS)),
        )

        assertThat(provider.coinHistory()).isEmpty()
    }

    @Test
    fun `escrow create is mapped with its amount`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "EscrowCreate",
                destination = COUNTERPARTY,
                amount = XrpTransactionAmount.Drops(DROPS),
            ),
        )

        val item = provider.coinHistory().single()

        assertThat(item.amount)
            .isEqualTo(Amount(value = BigDecimal("1.500000"), blockchain = Blockchain.XRP, type = AmountType.Coin))
        assertThat(item.isOutgoing).isTrue()
    }

    @Test
    fun `escrow create of an issued currency is mapped in token history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(transactionType = "EscrowCreate", amount = issued(BigDecimal("7.5"))),
        )

        assertThat(provider.tokenHistory().single().amount)
            .isEqualTo(Amount(value = BigDecimal("7.5"), token = TOKEN))
    }

    @Test
    fun `escrow finish is kept in coin history with no amount`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(operation("EscrowFinish"))

        val item = provider.coinHistory().single()

        assertThat(item.amount.value).isEqualTo(BigDecimal.ZERO)
        assertThat(item.type).isEqualTo(TransactionHistoryItem.TransactionType.ContractMethodName("EscrowFinish"))
    }

    @Test
    fun `escrow cancel and offer cancel are kept in coin history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation("EscrowCancel"),
            operation("OfferCancel"),
        )

        assertThat(provider.coinHistory().map { it.type }).containsExactly(
            TransactionHistoryItem.TransactionType.ContractMethodName("EscrowCancel"),
            TransactionHistoryItem.TransactionType.ContractMethodName("OfferCancel"),
        )
    }

    @Test
    fun `account delete is kept in coin history with its destination`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(transactionType = "AccountDelete", destination = COUNTERPARTY),
        )

        val item = provider.coinHistory().single()

        assertThat(item.amount.value).isEqualTo(BigDecimal.ZERO)
        assertThat(item.destinationType).isEqualTo(
            TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(COUNTERPARTY),
            ),
        )
    }

    @Test
    fun `offer selling coin is outgoing and declares no moved amount`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "OfferCreate",
                takerGets = XrpTransactionAmount.Drops(DROPS),
                takerPays = issued(BigDecimal("10")),
            ),
        )

        val item = provider.coinHistory().single()

        assertThat(item.amount.value).isEqualTo(BigDecimal.ZERO)
        assertThat(item.isOutgoing).isTrue()
    }

    @Test
    fun `offer buying coin is incoming`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "OfferCreate",
                takerGets = issued(BigDecimal("10")),
                takerPays = XrpTransactionAmount.Drops(DROPS),
            ),
        )

        assertThat(provider.coinHistory().single().isOutgoing).isFalse()
    }

    @Test
    fun `offer buying the token is incoming in token history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "OfferCreate",
                takerGets = XrpTransactionAmount.Drops(DROPS),
                takerPays = issued(BigDecimal("10")),
            ),
        )

        val item = provider.tokenHistory().single()

        assertThat(item.amount).isEqualTo(Amount(value = BigDecimal.ZERO, token = TOKEN))
        assertThat(item.isOutgoing).isFalse()
    }

    @Test
    fun `offer of another token is skipped in token history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "OfferCreate",
                takerGets = XrpTransactionAmount.Drops(DROPS),
                takerPays = issued(value = BigDecimal("10"), issuer = "rOtherIssuer"),
            ),
        )

        assertThat(provider.tokenHistory()).isEmpty()
    }

    @Test
    fun `clawback of the wallet funds is outgoing to the issuer`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "Clawback",
                account = ISSUER,
                // The clawed back funds are identified by the account they are taken from
                amount = issued(value = BigDecimal("4"), issuer = WALLET),
            ),
        )

        val item = provider.tokenHistory().single()

        assertThat(item.amount).isEqualTo(Amount(value = BigDecimal("4"), token = TOKEN))
        assertThat(item.isOutgoing).isTrue()
        assertThat(item.destinationType).isEqualTo(
            TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(ISSUER),
            ),
        )
    }

    @Test
    fun `clawback of another token is skipped`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation(
                transactionType = "Clawback",
                account = "rOtherIssuer",
                amount = issued(value = BigDecimal("4"), issuer = WALLET),
            ),
        )

        assertThat(provider.tokenHistory()).isEmpty()
    }

    @Test
    fun `types without currency data are skipped in token history`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(
            operation("OfferCancel"),
            operation("EscrowFinish"),
            operation("EscrowCancel"),
            operation(transactionType = "AccountDelete", destination = COUNTERPARTY),
        )

        assertThat(provider.tokenHistory()).isEmpty()
    }

    // endregion

    // region pagination

    @Test
    fun `marker is encoded into the next page`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns Result.Success(
            XrpAccountTxResponse(
                transactions = listOf(payment()),
                marker = XrpTransactionMarker(ledger = 98_765, seq = 4),
            ),
        )

        val result = provider.getTransactionsHistory(coinRequest()) as Result.Success

        assertThat(result.data.nextPage).isEqualTo(Page.Next("98765:4"))
    }

    @Test
    fun `absent marker means the last page`() = runTest {
        coEvery { networkProvider.getAccountTransactions(any()) } returns success(payment())

        val result = provider.getTransactionsHistory(coinRequest()) as Result.Success

        assertThat(result.data.nextPage).isEqualTo(Page.LastPage)
    }

    @Test
    fun `page to load is decoded back into the marker`() = runTest {
        val requests = mutableListOf<XrpAccountTxRequest>()
        coEvery { networkProvider.getAccountTransactions(capture(requests)) } returns success()

        provider.getTransactionsHistory(coinRequest(page = Page.Next("98765:4")))

        assertThat(requests.single()).isEqualTo(
            XrpAccountTxRequest(
                address = WALLET,
                limit = PAGE_SIZE,
                marker = XrpTransactionMarker(ledger = 98_765, seq = 4),
            ),
        )
    }

    @Test
    fun `malformed page to load is ignored`() = runTest {
        val requests = mutableListOf<XrpAccountTxRequest>()
        coEvery { networkProvider.getAccountTransactions(capture(requests)) } returns success()

        provider.getTransactionsHistory(coinRequest(page = Page.Next("broken")))

        assertThat(requests.single().marker).isNull()
    }

    @Test
    fun `failed request is propagated`() = runTest {
        val error = BlockchainSdkError.CustomError("failed")
        coEvery { networkProvider.getAccountTransactions(any()) } returns Result.Failure(error)

        val result = provider.getTransactionsHistory(coinRequest())

        assertThat(result).isEqualTo(Result.Failure(error))
    }

    // endregion

    private suspend fun XrpTransactionHistoryProvider.coinHistory(): List<TransactionHistoryItem> {
        val result = getTransactionsHistory(coinRequest()) as Result.Success
        return result.data.items
    }

    private suspend fun XrpTransactionHistoryProvider.tokenHistory(
        token: Token = TOKEN,
    ): List<TransactionHistoryItem> {
        val request = coinRequest().copy(filterType = TransactionHistoryRequest.FilterType.Contract(token))
        val result = getTransactionsHistory(request) as Result.Success
        return result.data.items
    }

    private fun coinRequest(page: Page = Page.Initial) = TransactionHistoryRequest(
        address = WALLET,
        decimals = Blockchain.XRP.decimals(),
        page = page,
        pageSize = PAGE_SIZE,
        filterType = TransactionHistoryRequest.FilterType.Coin,
    )

    private fun success(vararg transactions: XrpTransaction) = Result.Success(
        XrpAccountTxResponse(transactions = transactions.toList(), marker = null),
    )

    private fun payment(
        account: String = WALLET,
        destination: String? = COUNTERPARTY,
        amount: XrpTransactionAmount? = XrpTransactionAmount.Drops(DROPS),
        feeInDrops: BigDecimal? = FEE_DROPS,
        isValidated: Boolean = true,
        transactionResult: String? = "tesSUCCESS",
    ) = XrpTransaction(
        hash = HASH,
        account = account,
        destination = destination,
        amount = amount,
        limitAmount = null,
        feeInDrops = feeInDrops,
        transactionType = "Payment",
        date = RIPPLE_DATE,
        isValidated = isValidated,
        transactionResult = transactionResult,
    )

    private fun trustSet(limitAmount: XrpIssuedCurrencyAmount = issuedAmount(BigDecimal("100"))) = XrpTransaction(
        hash = HASH,
        account = WALLET,
        destination = null,
        amount = null,
        limitAmount = limitAmount,
        feeInDrops = FEE_DROPS,
        transactionType = "TrustSet",
        date = RIPPLE_DATE,
        isValidated = true,
        transactionResult = "tesSUCCESS",
    )

    private fun operation(
        transactionType: String,
        account: String = WALLET,
        destination: String? = null,
        amount: XrpTransactionAmount? = null,
        takerGets: XrpTransactionAmount? = null,
        takerPays: XrpTransactionAmount? = null,
    ) = XrpTransaction(
        hash = HASH,
        account = account,
        destination = destination,
        amount = amount,
        limitAmount = null,
        takerGets = takerGets,
        takerPays = takerPays,
        feeInDrops = FEE_DROPS,
        transactionType = transactionType,
        date = RIPPLE_DATE,
        isValidated = true,
        transactionResult = "tesSUCCESS",
    )

    private fun issuedAmount(
        value: BigDecimal,
        currency: String = CURRENCY,
        issuer: String = ISSUER,
    ): XrpIssuedCurrencyAmount {
        return XrpIssuedCurrencyAmount(currency = currency, issuer = issuer, value = value)
    }

    private fun issued(value: BigDecimal, currency: String = CURRENCY, issuer: String = ISSUER) =
        XrpTransactionAmount.IssuedCurrency(issuedAmount(value = value, currency = currency, issuer = issuer))

    private companion object {

        const val WALLET = "rWalletAddress0000000000000000000"
        const val COUNTERPARTY = "rCounterparty000000000000000000000"
        const val HASH = "9A1F0C0F0E0D0C0B0A09080706050403020100FFEEDDCCBBAA99887766554433221100"
        const val CURRENCY = "USD"
        const val ISSUER = "rIssuerAddress0000000000000000000"
        const val PAGE_SIZE = 20

        /** `2025-08-19T12:00:00Z` in the Ripple Epoch, i.e. `1_755_609_600` in the Unix Epoch */
        const val RIPPLE_DATE = 808_924_800L

        val DROPS: BigDecimal = BigDecimal("1500000")
        val FEE_DROPS: BigDecimal = BigDecimal("12")

        val TOKEN = Token(
            name = "USD",
            symbol = "USD",
            contractAddress = "$CURRENCY.$ISSUER",
            decimals = 15,
        )
    }
}