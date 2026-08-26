package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.psbt.DefaultPsbtProvider
import com.tangem.blockchain.common.psbt.PsbtProviderFactory
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.extensions.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Routing tests for [PsbtProviderFactory] across Litecoin/Dogecoin/Dash/Bitcoin Cash, plus negative
 * routing for Ravencoin (unsupported).
 */
internal class PsbtProviderMultiChainTest {

    @Before
    fun setup() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `factory returns working provider for Litecoin`() {
        assertChainDerivesSignInput(Blockchain.Litecoin)
    }

    @Test
    fun `factory returns working provider for Dogecoin`() {
        assertChainDerivesSignInput(Blockchain.Dogecoin)
    }

    @Test
    fun `factory returns working provider for Dash`() {
        assertChainDerivesSignInput(Blockchain.Dash)
    }

    private fun assertChainDerivesSignInput(blockchain: Blockchain) {
        // Given — a wallet owning the legacy P2PKH address of PsbtTestFixtures.ownerPublicKey on `blockchain`
        val ownerAddress = BitcoinAddressService(blockchain).makeLegacyAddress(PsbtTestFixtures.ownerPublicKey).value
        val wallet = Wallet(
            blockchain = blockchain,
            addresses = setOf(Address(ownerAddress, AddressType.Legacy)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        val provider = PsbtProviderFactory.make(blockchain, wallet, mockk<BitcoinNetworkProvider>(relaxed = true))
        val psbtBase64 = PsbtTestFixtures.serialize(PsbtTestFixtures.singleP2pkhLegacyInputPsbt())

        // When
        val result = provider.deriveSignInputs(psbtBase64)

        // Then — exactly one input, matching the wallet's own address
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val inputs = (result as Result.Success).data
        assertThat(inputs).hasSize(1)
        assertThat(inputs.single().address).isEqualTo(ownerAddress)
    }

    @Test
    fun `factory returns Default (no-op) for Ravencoin`() {
        val wallet = anyWallet(Blockchain.Ravencoin)
        val provider = PsbtProviderFactory.make(Blockchain.Ravencoin, wallet, mockk(relaxed = true))

        assertThat(provider).isSameInstanceAs(DefaultPsbtProvider)
        assertThat(provider.deriveSignInputs("x")).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `factory returns working provider for BitcoinCash`() {
        // Given — a wallet owning the CashAddr of PsbtTestFixtures.ownerPublicKey
        val ownerAddress = BitcoinCashAddressService(
            Blockchain.BitcoinCash,
        ).makeAddress(PsbtTestFixtures.ownerPublicKey)
        val wallet = Wallet(
            blockchain = Blockchain.BitcoinCash,
            addresses = setOf(Address(ownerAddress, AddressType.Default)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        val provider = PsbtProviderFactory.make(
            Blockchain.BitcoinCash,
            wallet,
            mockk<BitcoinNetworkProvider>(relaxed = true),
        )
        val psbtBase64 = PsbtTestFixtures.serialize(PsbtTestFixtures.singleP2pkhLegacyInputPsbt())

        // When
        val result = provider.deriveSignInputs(psbtBase64)

        // Then — exactly one input, matching the wallet's own CashAddr, sighash FORKID (0x41)
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val inputs = (result as Result.Success).data
        assertThat(inputs).hasSize(1)
        assertThat(inputs.single().address).isEqualTo(ownerAddress)
        assertThat(inputs.single().sighashTypes).isEqualTo(listOf(PsbtSighash.ALL_FORKID))
    }

    private fun anyWallet(blockchain: Blockchain): Wallet = Wallet(
        blockchain = blockchain,
        addresses = setOf(Address("dummy", AddressType.Default)),
        publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
        tokens = emptySet(),
    )
}