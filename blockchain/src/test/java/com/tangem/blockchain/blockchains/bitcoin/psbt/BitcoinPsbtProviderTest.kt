package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
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
}