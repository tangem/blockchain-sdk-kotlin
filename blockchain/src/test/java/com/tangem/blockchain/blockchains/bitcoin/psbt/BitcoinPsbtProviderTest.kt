package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.ScriptElt
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BitcoinPsbtProvider.parsePsbtOutputs].
 */
internal class BitcoinPsbtProviderTest {

    private val legacyAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
    private val segwitAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"

    // P2SH address owning the single input of [realSwapPsbtBase64]
    // (decoded from witness_utxo script a914 23e522dfc6656a8fda3d47b4fa53f7585ac758cd 87).
    private val swapInputP2shAddress = "34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo"

    // Real "naked" provider response (before our commission): LI.FI Bitcoin DEX swap PSBT.
    // 1 P2SH input (witness_utxo), 4 outputs (P2TR recipient, OP_RETURN memo, P2SH change, P2WPKH).
    private val realSwapPsbtBase64 = "cHNidP8BALICAAAAAe58GaGJV4GraMwH6COxzohpDo5MaxaBCgjQ9YYaY9/XAAAAAA" +
        "D9////BHw4DwAAAAAAIlEg/45BAJwUKjrOsE6IzCxT3HYZYrc5sZk+EHqlFUQgB5QAAAAAAAAAAAxqCj18bGlmaYJv2CSPiN6QLgA" +
        "AABepFCPlIt/GZWqP2j1HtPpT91hax1jNh8QJAAAAAAAAFgAUmrEFzl9lzJ0+UeSyQD7vGDHrW/0AAAAAAAEBIADQ7ZAuAAAAF6kU" +
        "I+Ui38Zlao/aPUe0+lP3WFrHWM2HAAAAAAA="

    private lateinit var provider: BitcoinPsbtProvider

    @Before
    fun setup() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }

        val wallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(Address(legacyAddress, AddressType.Legacy)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        provider = BitcoinPsbtProvider(wallet = wallet, networkProvider = mockk<BitcoinNetworkProvider>(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `parsePsbtOutputs returns decoded recipients and amounts`() {
        // Given
        val psbtBase64 = buildPsbtBase64(
            legacyAddress to 100_000L,
            segwitAddress to 250_000L,
        )

        // When
        val result = provider.parsePsbtOutputs(psbtBase64)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val outputs = (result as Result.Success).data
        assertThat(outputs).hasSize(2)
        assertThat(outputs[0].address).isEqualTo(legacyAddress)
        assertThat(outputs[0].amountSatoshi).isEqualTo(100_000L)
        assertThat(outputs[1].address).isEqualTo(segwitAddress)
        assertThat(outputs[1].amountSatoshi).isEqualTo(250_000L)
    }

    @Test
    fun `parsePsbtOutputs returns failure for invalid base64 input`() {
        // When
        val result = provider.parsePsbtOutputs("not-a-valid-psbt")

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `parsePsbtOutputs returns failure for non-bitcoin blockchain`() {
        // Given an altcoin wallet manager that inherits BitcoinPsbtProvider (e.g. Dogecoin)
        val altcoinWallet = Wallet(
            blockchain = Blockchain.Dogecoin,
            addresses = setOf(Address(legacyAddress, AddressType.Legacy)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        val altcoinProvider = BitcoinPsbtProvider(
            wallet = altcoinWallet,
            networkProvider = mockk(relaxed = true),
        )
        val psbtBase64 = buildPsbtBase64(legacyAddress to 100_000L)

        // When
        val result = altcoinProvider.parsePsbtOutputs(psbtBase64)

        // Then — must not decode altcoin outputs with Bitcoin chain params
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    // region deriveSignInputs

    @Test
    fun `deriveSignInputs returns our input from real provider psbt`() {
        // Given — a real "naked" provider PSBT (LI.FI Bitcoin DEX swap) with a single P2SH input
        // whose UTXO belongs to the wallet (address 34xp...Twseo).
        val walletWithSwapInput = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(Address(swapInputP2shAddress, AddressType.Default)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        val swapProvider = BitcoinPsbtProvider(
            wallet = walletWithSwapInput,
            networkProvider = mockk(relaxed = true),
        )

        // When
        val result = swapProvider.deriveSignInputs(realSwapPsbtBase64)

        // Then — exactly the one input that belongs to us, default SIGHASH_ALL
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val inputs = (result as Result.Success).data
        assertThat(inputs).containsExactly(
            SignInput(address = swapInputP2shAddress, index = 0, sighashTypes = listOf(SIGHASH_ALL)),
        )
    }

    @Test
    fun `deriveSignInputs returns failure when no input belongs to the wallet`() {
        // Given — wallet that does NOT own any input of the real swap PSBT
        // (default setup wallet holds legacyAddress, not the P2SH swap input)

        // When
        val result = provider.deriveSignInputs(realSwapPsbtBase64)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `deriveSignInputs returns failure for invalid base64 input`() {
        // When
        val result = provider.deriveSignInputs("not-a-valid-psbt")

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `deriveSignInputs returns failure for non-bitcoin blockchain`() {
        // Given — altcoin wallet that inherits BitcoinPsbtProvider
        val altcoinWallet = Wallet(
            blockchain = Blockchain.Dogecoin,
            addresses = setOf(Address(swapInputP2shAddress, AddressType.Default)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        val altcoinProvider = BitcoinPsbtProvider(wallet = altcoinWallet, networkProvider = mockk(relaxed = true))

        // When
        val result = altcoinProvider.deriveSignInputs(realSwapPsbtBase64)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    // endregion

    private fun buildPsbtBase64(vararg outputs: Pair<String, Long>): String {
        val txOuts = outputs.map { (address, amount) ->
            val script = when (val r = Bitcoin.addressToPublicKeyScript(Block.LivenetGenesisBlock.hash, address)) {
                is Either.Right -> r.value
                is Either.Left -> error("Failed to build script for $address: ${r.value}")
            }
            TxOut(Satoshi(amount), script as List<ScriptElt>)
        }
        val tx = Transaction(version = 2L, txIn = emptyList(), txOut = txOuts, lockTime = 0L)
        val psbt = Psbt(tx)
        val bytes = Psbt.write(psbt).toByteArray()
        return java.util.Base64.getEncoder().encodeToString(bytes)
    }

    private companion object {
        const val SIGHASH_ALL = 1
    }
}