package com.tangem.blockchain.blockchains.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.network.ExternalAddressBalances
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal

/**
 * Balances of an address the wallet does not own must be read by the plain path only.
 *
 * `getInfo` and `getTokensBalance` resolve token balances through the yield-supply provider, which is built around
 * this wallet: on a foreign address they can answer with the wallet's own yield balance instead of that address's
 * token balance. The tests below fail if the manager is ever "simplified" back onto either of them.
 */
internal class ExternalAddressBalanceProviderTest {

    private val networkProvider: EthereumNetworkProvider = mockk()

    private val walletManager = EthereumWalletManager(
        wallet = wallet(),
        transactionBuilder = mockk(),
        networkProvider = networkProvider,
        supportsENS = false,
    )

    @Test
    fun `GIVEN an external address WHEN balances requested THEN the provider answers for that address`() = runBlocking {
        val expected = ExternalAddressBalances(
            coinBalance = BigDecimal("1.5"),
            tokenBalances = listOf(Amount(token = TOKEN, value = BigDecimal("10"))),
        )
        coEvery {
            networkProvider.getExternalAddressBalances(SAFE_ADDRESS, setOf(TOKEN))
        } returns Result.Success(expected)

        val actual = walletManager.getExternalAddressBalances(address = SAFE_ADDRESS, tokens = setOf(TOKEN))

        assertThat((actual as Result.Success).data).isSameInstanceAs(expected)
        coVerify(exactly = 1) { networkProvider.getExternalAddressBalances(SAFE_ADDRESS, setOf(TOKEN)) }
    }

    @Test
    fun `GIVEN an external address WHEN balances requested THEN the wallet-scoped reads are never used`() =
        runBlocking {
            coEvery { networkProvider.getExternalAddressBalances(any(), any()) } returns Result.Success(
                ExternalAddressBalances(coinBalance = BigDecimal.ZERO, tokenBalances = emptyList()),
            )

            walletManager.getExternalAddressBalances(address = SAFE_ADDRESS, tokens = setOf(TOKEN))

            // Both would route token balances through this wallet's yield-supply module
            coVerify(exactly = 0) { networkProvider.getInfo(any(), any()) }
            coVerify(exactly = 0) { networkProvider.getTokensBalance(any(), any()) }
        }

    @Test
    fun `GIVEN a provider failure WHEN balances requested THEN the failure is passed through`() = runBlocking {
        val failure = Result.Failure(com.tangem.blockchain.common.BlockchainSdkError.CustomError("no network"))
        coEvery { networkProvider.getExternalAddressBalances(any(), any()) } returns failure

        val actual = walletManager.getExternalAddressBalances(address = SAFE_ADDRESS, tokens = emptySet())

        assertThat(actual).isSameInstanceAs(failure)
    }

    private fun wallet(): Wallet = Wallet(
        blockchain = Blockchain.Ethereum,
        addresses = setOf(Address(value = OWN_ADDRESS)),
        publicKey = Wallet.PublicKey(seedKey = ByteArray(size = 32), derivationType = null),
        tokens = emptySet(),
    )

    private companion object {
        const val OWN_ADDRESS = "0xE31C6A9eE83A0f6f2e44dDcb9837B162802DeC12"
        const val SAFE_ADDRESS = "0xe7411b2aB7b76a11ca0E6174697714983fC09Cb2"

        val TOKEN = Token(
            name = "Tether",
            symbol = "USDT",
            contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            decimals = 6,
        )
    }
}