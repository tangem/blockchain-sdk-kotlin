package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.psbt.PsbtProviderFactory
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.operations.sign.SignData
import fr.acinq.bitcoin.psbt.Input
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * End-to-end test for [BitcoinPsbtProvider] wired up for Litecoin via [PsbtProviderFactory]: exercises
 * the bech32 (SegWit) address path + BIP-143 witness signing/finalization on a non-Bitcoin chain,
 * closing the one untested new-chain signing path (Bitcoin Cash's e2e test in
 * [com.tangem.blockchain.blockchains.bitcoincash.psbt.BitcoinCashPsbtProviderTest] only covers the
 * legacy/non-witness finalization path).
 */
internal class LitecoinPsbtProviderTest {

    private val ownerSegwitAddress =
        BitcoinAddressService(Blockchain.Litecoin).makeSegwitAddress(PsbtTestFixtures.ownerPublicKey).value

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

    private fun ltcWallet(): Wallet = Wallet(
        blockchain = Blockchain.Litecoin,
        addresses = setOf(
            Address(
                BitcoinAddressService(Blockchain.Litecoin).makeSegwitAddress(PsbtTestFixtures.ownerPublicKey).value,
                AddressType.Default,
            ),
        ),
        publicKey = Wallet.PublicKey(seedKey = PsbtTestFixtures.ownerPublicKey, derivationType = null),
        tokens = emptySet(),
    )

    @Test
    fun `deriveSignInputs returns the owner SegWit input for Litecoin`() {
        // Given
        val provider = PsbtProviderFactory.make(Blockchain.Litecoin, ltcWallet(), mockk(relaxed = true))
        val psbtBase64 = PsbtTestFixtures.serialize(PsbtTestFixtures.singleP2wpkhInputPsbt())

        // When
        val result = provider.deriveSignInputs(psbtBase64)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val inputs = (result as Result.Success).data
        assertThat(inputs).containsExactly(
            SignInput(address = ownerSegwitAddress, index = 0, sighashTypes = listOf(PsbtSighash.ALL)),
        )
    }

    @Test
    fun `signPsbt finalizes the LTC SegWit input as a witness (not legacy)`() = runBlocking {
        // Given
        val provider = PsbtProviderFactory.make(Blockchain.Litecoin, ltcWallet(), mockk(relaxed = true))
        val psbtBase64 = PsbtTestFixtures.serialize(PsbtTestFixtures.singleP2wpkhInputPsbt())
        val signInputs = (provider.deriveSignInputs(psbtBase64) as Result.Success).data

        // When
        val signedResult = provider.signPsbt(psbtBase64, signInputs, DeterministicFakeSigner)

        // Then
        assertThat(signedResult).isInstanceOf(Result.Success::class.java)
        val signedPsbtBase64 = (signedResult as Result.Success).data

        val signedPsbt = when (val decoded = Psbt.read(java.util.Base64.getDecoder().decode(signedPsbtBase64))) {
            is Either.Right -> decoded.value
            is Either.Left -> error("Failed to decode signed LTC PSBT: ${decoded.value}")
        }

        // Witness path, not legacy scriptSig finalization.
        assertThat(signedPsbt.inputs[0]).isInstanceOf(Input.WitnessInput.FinalizedWitnessInput::class.java)
    }

    private object DeterministicFakeSigner : TransactionSigner {
        private val fixedSignature = ByteArray(64) { 0x01 }

        override suspend fun sign(
            hashes: List<ByteArray>,
            publicKey: Wallet.PublicKey,
        ): CompletionResult<List<ByteArray>> {
            return CompletionResult.Success(hashes.map { fixedSignature })
        }

        override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
            return CompletionResult.Success(fixedSignature)
        }

        override suspend fun multiSign(
            dataToSign: List<SignData>,
            publicKey: Wallet.PublicKey,
        ): CompletionResult<Map<ByteArray, ByteArray>> {
            return CompletionResult.Success(emptyMap())
        }
    }
}