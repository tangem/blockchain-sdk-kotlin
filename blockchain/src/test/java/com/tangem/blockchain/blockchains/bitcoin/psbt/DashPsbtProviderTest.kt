package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
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
 * Integration test for [PsbtProviderFactory]-wired Dash PSBT support, driven by a real
 * Express/SwapKit swap PSBT (see `docs/superpowers/specs/[REDACTED_TASK_KEY]-real-swap-samples.md`, Dash
 * section). Validates the LTC/DOGE/DASH `DefaultSighashStrategy` path (sighash 0x01) on a real
 * swap payload, not just synthetic fixtures.
 */
internal class DashPsbtProviderTest {

    // Real Dash swap PSBT: single legacy (nonWitnessUtxo) P2PKH input, 2 outputs
    // (out0 57274221 to recipient, out1 11172144080903 change back to owner).
    private val realDashPsbtBase64 = "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"

    private val ownerAddress = "XtVaSYnohTd9ihhhiwZmMFgghe2mptJax1"

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
    fun `deriveSignInputs returns the owner input with default sighash from a real Dash swap PSBT`() {
        // Given
        val wallet = Wallet(
            blockchain = Blockchain.Dash,
            addresses = setOf(Address(ownerAddress, AddressType.Legacy)),
            publicKey = Wallet.PublicKey(seedKey = ByteArray(65) { 0x04 }, derivationType = null),
            tokens = emptySet(),
        )
        val provider = PsbtProviderFactory.make(Blockchain.Dash, wallet, mockk(relaxed = true))

        // When
        val result = provider.deriveSignInputs(realDashPsbtBase64)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val inputs = (result as Result.Success).data
        assertThat(inputs).containsExactly(
            SignInput(address = ownerAddress, index = 0, sighashTypes = listOf(PsbtSighash.ALL)),
        )
    }
}