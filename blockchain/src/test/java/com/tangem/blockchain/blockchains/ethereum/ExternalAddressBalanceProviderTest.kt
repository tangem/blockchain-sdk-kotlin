package com.tangem.blockchain.blockchains.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * The provider keeps the state of an address the user does not control the way a wallet manager keeps its wallet: an
 * update takes balances and transactions from the chain's network service, the host may enrich the transactions, and
 * every other network or history call is the underlying service's, untouched.
 */
internal class ExternalAddressBalanceProviderTest {

    private val networkProvider: EthereumNetworkProvider = mockk()
    private val transactionHistoryProvider: TransactionHistoryProvider = mockk()

    private val provider = ExternalAddressBalanceProvider(
        blockchain = Blockchain.Ethereum,
        address = SAFE_ADDRESS,
        networkProvider = networkProvider,
        transactionHistoryProvider = transactionHistoryProvider,
    )

    @Test
    fun `GIVEN a successful read WHEN update THEN the provider holds the coin and token balances`() = runBlocking {
        provider.addTokens(listOf(TOKEN))
        coEvery { networkProvider.getInfo(SAFE_ADDRESS, setOf(TOKEN)) } returns Result.Success(
            info(coinBalance = BigDecimal("1.5"), tokenBalances = listOf(Amount(token = TOKEN, value = BigDecimal("10")))),
        )

        val actual = provider.update()

        val data = (actual as Result.Success).data
        assertThat(data.address).isEqualTo(SAFE_ADDRESS)
        assertThat(data.amounts[AmountType.Coin]?.value).isEqualTo(BigDecimal("1.5"))
        assertThat(data.amounts[AmountType.Token(TOKEN)]?.value).isEqualTo(BigDecimal("10"))
        assertThat(provider.amounts[AmountType.Coin]?.value).isEqualTo(BigDecimal("1.5"))
        assertThat(provider.amounts[AmountType.Token(TOKEN)]?.value).isEqualTo(BigDecimal("10"))
    }

    @Test
    fun `GIVEN a token removed WHEN update THEN it is neither requested nor kept`() = runBlocking {
        provider.addTokens(listOf(TOKEN))
        coEvery { networkProvider.getInfo(SAFE_ADDRESS, setOf(TOKEN)) } returns Result.Success(
            info(coinBalance = BigDecimal.ONE, tokenBalances = listOf(Amount(token = TOKEN, value = BigDecimal.TEN))),
        )
        provider.update()

        provider.removeToken(TOKEN)
        coEvery { networkProvider.getInfo(SAFE_ADDRESS, emptySet()) } returns Result.Success(
            info(coinBalance = BigDecimal.ONE, tokenBalances = emptyList()),
        )
        val actual = provider.update()

        assertThat((actual as Result.Success).data.amounts.keys).containsExactly(AmountType.Coin)
        coVerify(exactly = 1) { networkProvider.getInfo(SAFE_ADDRESS, emptySet()) }
    }

    @Test
    fun `GIVEN a failed read WHEN update THEN the failure is returned and the amounts are kept`() = runBlocking {
        coEvery { networkProvider.getInfo(any(), any()) } returns Result.Success(
            info(coinBalance = BigDecimal("2"), tokenBalances = emptyList()),
        )
        provider.update()
        val error = BlockchainSdkError.CustomError("no network")
        coEvery { networkProvider.getInfo(any(), any()) } returns Result.Failure(error)

        val actual = provider.update()

        assertThat((actual as Result.Failure).error).isSameInstanceAs(error)
        assertThat(provider.amounts[AmountType.Coin]?.value).isEqualTo(BigDecimal("2"))
    }

    @Test
    fun `GIVEN network reports recent transactions WHEN update THEN new ones are added and matching ones confirmed`() =
        runBlocking {
            provider.addTransactions(listOf(transaction(hash = "0x01")))
            coEvery { networkProvider.getInfo(any(), any()) } returns Result.Success(
                info(
                    coinBalance = BigDecimal.ZERO,
                    tokenBalances = emptyList(),
                    txCount = 5,
                    pendingTxCount = 6,
                    recentTransactions = listOf(
                        transaction(hash = "0x01", status = TransactionStatus.Confirmed),
                        transaction(hash = "0x02"),
                    ),
                ),
            )

            val actual = provider.update()

            val recent = (actual as Result.Success).data.recentTransactions
            assertThat(recent.map { it.hash }).containsExactly("0x01", "0x02").inOrder()
            assertThat(recent.first { it.hash == "0x01" }.status).isEqualTo(TransactionStatus.Confirmed)
            assertThat(recent.first { it.hash == "0x02" }.status).isEqualTo(TransactionStatus.Unconfirmed)
        }

    @Test
    fun `GIVEN nothing pending on chain WHEN update THEN the known transactions are confirmed`() = runBlocking {
        provider.addTransactions(listOf(transaction(hash = "0x01")))
        coEvery { networkProvider.getInfo(any(), any()) } returns Result.Success(
            info(coinBalance = BigDecimal.ZERO, tokenBalances = emptyList(), txCount = 7, pendingTxCount = 7),
        )

        val actual = provider.update()

        assertThat((actual as Result.Success).data.recentTransactions.single().status)
            .isEqualTo(TransactionStatus.Confirmed)
    }

    @Test
    fun `GIVEN pending on chain without details WHEN update THEN a placeholder transaction is added`() = runBlocking {
        coEvery { networkProvider.getInfo(any(), any()) } returns Result.Success(
            info(coinBalance = BigDecimal.ZERO, tokenBalances = emptyList(), txCount = 7, pendingTxCount = 8),
        )

        val actual = provider.update()

        val placeholder = (actual as Result.Success).data.recentTransactions.single()
        assertThat(placeholder.hash).isNull()
        assertThat(placeholder.status).isEqualTo(TransactionStatus.Unconfirmed)
    }

    @Test
    fun `GIVEN a transaction with a known hash WHEN addTransactions THEN the entry is replaced, not duplicated`() {
        provider.addTransactions(listOf(transaction(hash = "0x01")))

        provider.addTransactions(listOf(transaction(hash = "0x01", status = TransactionStatus.Confirmed)))

        assertThat(provider.recentTransactions).hasSize(1)
        assertThat(provider.recentTransactions.single().status).isEqualTo(TransactionStatus.Confirmed)
    }

    @Test
    fun `GIVEN an external address WHEN update THEN the wallet-scoped token read is never used`() = runBlocking {
        coEvery { networkProvider.getInfo(any(), any()) } returns Result.Success(
            info(coinBalance = BigDecimal.ZERO, tokenBalances = emptyList()),
        )

        provider.update()

        coVerify(exactly = 0) { networkProvider.getTokensBalance(any(), any()) }
    }

    @Test
    fun `GIVEN a network or history call WHEN made on the provider THEN it is the service's call`() = runBlocking {
        every { networkProvider.baseUrl } returns "https://rpc.example.org/"
        coEvery { networkProvider.getContractNonce(SAFE_ADDRESS) } returns Result.Success(BigInteger.valueOf(7))
        coEvery {
            transactionHistoryProvider.getTransactionHistoryState(SAFE_ADDRESS, TransactionHistoryRequest.FilterType.Coin)
        } returns TransactionHistoryState.NotImplemented

        val nonce = provider.getContractNonce(SAFE_ADDRESS)
        val history = provider.getTransactionHistoryState(SAFE_ADDRESS, TransactionHistoryRequest.FilterType.Coin)

        assertThat(provider.baseUrl).isEqualTo("https://rpc.example.org/")
        assertThat((nonce as Result.Success).data).isEqualTo(BigInteger.valueOf(7))
        assertThat(history).isEqualTo(TransactionHistoryState.NotImplemented)
    }

    private fun info(
        coinBalance: BigDecimal,
        tokenBalances: List<Amount>,
        txCount: Long = 0,
        pendingTxCount: Long = 0,
        recentTransactions: List<TransactionData.Uncompiled>? = null,
    ): EthereumInfoResponse {
        return EthereumInfoResponse(
            coinBalance = coinBalance,
            tokenBalances = tokenBalances,
            txCount = txCount,
            pendingTxCount = pendingTxCount,
            recentTransactions = recentTransactions,
        )
    }

    private fun transaction(
        hash: String,
        status: TransactionStatus = TransactionStatus.Unconfirmed,
    ): TransactionData.Uncompiled {
        return TransactionData.Uncompiled(
            amount = Amount(BigDecimal.ONE, Blockchain.Ethereum, AmountType.Coin),
            fee = null,
            sourceAddress = SAFE_ADDRESS,
            destinationAddress = OWNER_ADDRESS,
            status = status,
            hash = hash,
        )
    }

    private companion object {
        const val OWNER_ADDRESS = "0xE31C6A9eE83A0f6f2e44dDcb9837B162802DeC12"
        const val SAFE_ADDRESS = "0xe7411b2aB7b76a11ca0E6174697714983fC09Cb2"

        val TOKEN = Token(
            name = "Tether",
            symbol = "USDT",
            contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            decimals = 6,
        )
    }
}