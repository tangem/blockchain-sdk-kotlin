package com.tangem.blockchain.blockchains.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.tron.network.TronGetAccountResponse
import com.tangem.blockchain.blockchains.tron.network.TronNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The node answers `/wallet/getaccount` with an empty JSON for an address whose account has never
 * been activated, so the presence of the `address` field is the activation signal. A failed
 * request must stay a failure: the app hides features (e.g. Tron gasless) for a not-activated
 * account, and a node outage must not be reported as "no account".
 */
internal class TronAccountActivationTest {

    private val walletAddress = "TXdDX2sqnzJidbTvH8j63WG1cKntGPgSBz"

    private val provider: TronNetworkProvider = mockk {
        every { baseUrl } returns "https://api.tron/"
    }

    private val service = TronNetworkService(
        rpcNetworkProviders = listOf(provider),
        blockchain = Blockchain.Tron,
    )

    @Test
    fun `GIVEN account response with address WHEN isAccountActivated THEN true`() = runTest {
        // Arrange
        coEvery { provider.getAccount(walletAddress) } returns Result.Success(
            TronGetAccountResponse(balance = 0, address = "41" + "00".repeat(20), trc20 = null),
        )

        // Act
        val actual = service.isAccountActivated(walletAddress)

        // Assert
        assertThat(actual).isEqualTo(Result.Success(true))
    }

    @Test
    fun `GIVEN empty account response WHEN isAccountActivated THEN false`() = runTest {
        // Arrange
        coEvery { provider.getAccount(walletAddress) } returns Result.Success(
            TronGetAccountResponse(balance = null, address = null, trc20 = null),
        )

        // Act
        val actual = service.isAccountActivated(walletAddress)

        // Assert
        assertThat(actual).isEqualTo(Result.Success(false))
    }

    @Test
    fun `GIVEN node failure WHEN isAccountActivated THEN failure is propagated`() = runTest {
        // Arrange
        val error = BlockchainSdkError.CustomError("500")
        coEvery { provider.getAccount(walletAddress) } returns Result.Failure(error)

        // Act
        val actual = service.isAccountActivated(walletAddress)

        // Assert
        assertThat(actual).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `GIVEN node failure WHEN checkIfAccountExists THEN false`() = runTest {
        // Arrange
        coEvery { provider.getAccount(walletAddress) } returns Result.Failure(BlockchainSdkError.CustomError("500"))

        // Act
        val actual = service.checkIfAccountExists(walletAddress)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN wallet manager WHEN isAccountActivated THEN own wallet address is checked`() = runTest {
        // Arrange
        val networkService: TronNetworkService = mockk {
            every { host } returns "https://api.tron/"
            coEvery { isAccountActivated(walletAddress) } returns Result.Success(false)
        }
        val wallet: Wallet = mockk(relaxed = true) {
            every { address } returns walletAddress
        }
        val walletManager = TronWalletManager(
            wallet = wallet,
            transactionHistoryProvider = mockk<TransactionHistoryProvider>(relaxed = true),
            transactionBuilder = TronTransactionBuilder(),
            networkService = networkService,
        )

        // Act
        val actual = walletManager.isAccountActivated()

        // Assert
        assertThat(actual).isEqualTo(Result.Success(false))
    }
}