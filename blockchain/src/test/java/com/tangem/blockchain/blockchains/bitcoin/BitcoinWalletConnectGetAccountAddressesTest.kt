package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.address.BitcoinAddressProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.AddressIntention
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.Result
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BitcoinAddressProvider.getAddresses method.
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
    private lateinit var addressProvider: BitcoinAddressProvider

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

        addressProvider = BitcoinAddressProvider(wallet)
    }

    @Test
    fun `getAddresses returns all addresses when no intentions specified`() {
        // When
        val result = addressProvider.getAddresses(filterOptions = null)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val addresses = (result as Result.Success).data

        assertThat(addresses).hasSize(2)
        assertThat(addresses.map { it.address }).containsExactly(
            testLegacyAddress,
            testSegwitAddress,
        )
    }

    @Test
    fun `getAddresses returns empty array for ordinal-only intention`() {
        // When
        val result = addressProvider.getAddresses(filterOptions = listOf("ordinal"))

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val addresses = (result as Result.Success).data

        assertThat(addresses).isEmpty()
    }

    @Test
    fun `getAddresses returns addresses for payment intention`() {
        // When
        val result = addressProvider.getAddresses(filterOptions = listOf("payment"))

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val addresses = (result as Result.Success).data

        assertThat(addresses).hasSize(2)
        addresses.forEach { address ->
            val intention = address.metadata?.get("intention") as? String
            assertThat(intention).isEqualTo(AddressIntention.PAYMENT.toApiString())
        }
    }

    @Test
    fun `getAddresses returns addresses for mixed intentions`() {
        // When
        val result = addressProvider.getAddresses(filterOptions = listOf("payment", "ordinal"))

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val addresses = (result as Result.Success).data

        // Should return addresses because not ONLY ordinal
        assertThat(addresses).hasSize(2)
    }

    @Test
    fun `getAddresses includes both Legacy and SegWit addresses`() {
        // When
        val result = addressProvider.getAddresses(filterOptions = listOf("payment"))

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val addresses = (result as Result.Success).data

        val legacyAddress = addresses.find { it.address == testLegacyAddress }
        val segwitAddress = addresses.find { it.address == testSegwitAddress }

        assertThat(legacyAddress).isNotNull()
        assertThat(segwitAddress).isNotNull()
    }

    @Test
    fun `getAddresses does not expose publicKey by default`() {
        // When
        val result = addressProvider.getAddresses(filterOptions = listOf("payment"))

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val addresses = (result as Result.Success).data

        addresses.forEach { address ->
            assertThat(address.publicKey).isNull()
        }
    }
}