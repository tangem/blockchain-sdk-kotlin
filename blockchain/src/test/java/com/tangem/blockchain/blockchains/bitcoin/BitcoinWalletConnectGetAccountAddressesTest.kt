package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.BitcoinWalletConnectHandler
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.GetAccountAddressesRequest
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.Result
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BitcoinWalletConnectHandler.getAccountAddresses method.
 *
 * Tests cover:
 * - Default behavior (no intentions specified)
 * - Ordinal-only filter returns empty array
 * - Payment intention filter
 * - Multiple addresses (Legacy + SegWit)
 * - Address intention mapping
 */
internal class BitcoinWalletConnectGetAccountAddressesTest {

    private lateinit var wallet: Wallet
    private lateinit var walletManager: BitcoinWalletManager
    private lateinit var handler: BitcoinWalletConnectHandler

    private val testPublicKey = ByteArray(65) { 0x04 }
    private val testLegacyAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
    private val testSegwitAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"

    @Before
    fun setup() {
        // Setup wallet with multiple addresses
        wallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(
                Address(testLegacyAddress, AddressType.Legacy),
                Address(testSegwitAddress, AddressType.Default),
            ),
            publicKey = Wallet.PublicKey(
                seedKey = testPublicKey.copyOf(),
                derivationType = null,
            ),
            tokens = emptySet(),
        )

        // Mock wallet manager and network provider
        walletManager = mockk(relaxed = true)
        val networkProvider = mockk<BitcoinNetworkProvider>(relaxed = true)

        handler = BitcoinWalletConnectHandler(
            wallet = wallet,
            walletManager = walletManager,
            networkProvider = networkProvider,
        )
    }

    @Test
    fun `getAccountAddresses returns all addresses when no intentions specified`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = null,
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        assertThat(response.addresses).hasSize(2)
        assertThat(response.addresses.map { it.address }).containsExactly(
            testLegacyAddress,
            testSegwitAddress,
        )
    }

    @Test
    fun `getAccountAddresses returns empty array for ordinal-only intention`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = listOf("ordinal"),
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        assertThat(response.addresses).isEmpty()
    }

    @Test
    fun `getAccountAddresses returns addresses for payment intention`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = listOf("payment"),
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        assertThat(response.addresses).hasSize(2)
        response.addresses.forEach { address ->
            assertThat(address.intention).isEqualTo("payment")
        }
    }

    @Test
    fun `getAccountAddresses returns addresses for mixed intentions`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = listOf("payment", "ordinal"),
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        // Should return addresses because not ONLY ordinal
        assertThat(response.addresses).hasSize(2)
    }

    @Test
    fun `getAccountAddresses includes both Legacy and SegWit addresses`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = listOf("payment"),
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        val legacyAddress = response.addresses.find { it.address == testLegacyAddress }
        val segwitAddress = response.addresses.find { it.address == testSegwitAddress }

        assertThat(legacyAddress).isNotNull()
        assertThat(segwitAddress).isNotNull()
    }

    @Test
    fun `getAccountAddresses does not expose publicKey by default`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = listOf("payment"),
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        response.addresses.forEach { address ->
            assertThat(address.publicKey).isNull()
        }
    }

    @Test
    fun `getAccountAddresses handles invalid intention strings gracefully`() {
        // Given
        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = listOf("invalid_intention", "payment"),
        )

        // When
        val result = handler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        // Should still return addresses (invalid intentions are filtered out)
        assertThat(response.addresses).hasSize(2)
    }

    @Test
    fun `getAccountAddresses handles single address wallet`() {
        // Given
        val singleAddressWallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(Address(testSegwitAddress, AddressType.Default)),
            publicKey = Wallet.PublicKey(testPublicKey.copyOf(), null),
            tokens = emptySet(),
        )

        val networkProvider = mockk<BitcoinNetworkProvider>(relaxed = true)
        val singleAddressHandler = BitcoinWalletConnectHandler(
            wallet = singleAddressWallet,
            walletManager = walletManager,
            networkProvider = networkProvider,
        )

        val request = GetAccountAddressesRequest(
            account = testSegwitAddress,
            intentions = null,
        )

        // When
        val result = singleAddressHandler.getAccountAddresses(request)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val response = (result as Result.Success).data

        assertThat(response.addresses).hasSize(1)
        assertThat(response.addresses[0].address).isEqualTo(testSegwitAddress)
    }
}