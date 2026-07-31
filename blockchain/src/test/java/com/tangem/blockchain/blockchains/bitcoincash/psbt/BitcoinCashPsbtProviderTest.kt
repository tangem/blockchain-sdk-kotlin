package com.tangem.blockchain.blockchains.bitcoincash.psbt

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.psbt.PsbtTestFixtures
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.psbt.PsbtProviderFactory
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.operations.sign.SignData
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OP_PUSHDATA
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Input
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.psbt.UpdateFailure
import fr.acinq.bitcoin.utils.Either
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for [com.tangem.blockchain.blockchains.bitcoin.psbt.BitcoinPsbtProvider] wired up
 * for Bitcoin Cash via [PsbtProviderFactory], driven by a real Express/SwapKit swap PSBT (see
 * `docs/superpowers/specs/[REDACTED_TASK_KEY]-real-swap-samples.md` and
 * `docs/superpowers/specs/[REDACTED_TASK_KEY]-bch-spike-notes.md`).
 *
 * The owner address below (`bitcoincash:qp028nlln35nwnv5a9dssw9w57z5n765rgenr3suw6`) was independently
 * verified by CashAddr-encoding the input's prev P2PKH hash160 (`5ea3cfff9c69374d94e95b0838aea78549fb541a`,
 * extracted from prev scriptPubKey `76a9145ea3cfff9c69374d94e95b0838aea78549fb541a88ac`) with a
 * from-scratch (non-bitcoinj) CashAddr implementation — it matches the spec's `txFrom` verbatim.
 */
internal class BitcoinCashPsbtProviderTest {

    // Real BCH swap PSBT (Express QA swap response, txDetailsJson.txData). Single legacy
    // (nonWitnessUtxo) P2PKH input with explicit PSBT_IN_SIGHASH_TYPE = 0x41, 2 outputs.
    // See docs/superpowers/specs/[REDACTED_TASK_KEY]-bch-spike-notes.md for the decoded facts.
    private val realBchPsbtBase64 = "cHNidP8BAHcCAAAAAbjn+ZqPMkrnS9W31VCpc6Fv6qGPNLJmG0MMc2kuq401AgAAAAD/////Am3vaQMAAAAAGXapFKpUGcV2GjfcK1L1dxLg0WAlGPCPiKycdQBjAwAAABl2qRReo8//nGk3TZTpWwg4rqeFSftUGoisAAAAAAABAP2YAQEAAAACN/awtEDlWY0arLt1HxCK6djWgA4ZPIYo92MB2/R+VgYBAAAAa0gwRQIhAOj0HCt92+XlZTBf8kRcAM8sDoO1x2IsjzUSXR+CUs+GAiBOyhp2mdFxNIsWIdVB5WkIyrsqAcPfF8uEbJaz3vAg4UEhAlDxLZGUTsZ8XJEJPm6GYNByd1gU9833cDxfyjUdZkVu/////13x64t7ifOo59dFwEVVBAPxsY/DjZtpCY2Nh7Li4Ii5AQAAAGtIMEUCIQCfK2OVyR0lzEUDI44oZM0so1+w7kZy5E3ioapEKQeydgIgKQOteHrYyTaQSlGiAxh37ThZuyPPviwtyE2VMIPgH+RBIQJQ8S2RlE7GfFyRCT5uhmDQcndYFPfN93A8X8o1HWZFbv////8D4MN5AAAAAAAZdqkU3TpcdbgMWZZcpxfVBakbUKT09+WIrGBImAAAAAAAGXapFDAqIo3Qq4HVxVDRhfQQr75OIXFLiKzrZWpmAwAAABl2qRReo8//nGk3TZTpWwg4rqeFSftUGoisAAAAAAEDBEEAAAAAAAA="

    // txFrom from the same Express swap response — verified independently (see class doc).
    private val ownerAddress = "bitcoincash:qp028nlln35nwnv5a9dssw9w57z5n765rgenr3suw6"

    // Real BCH swap PSBT with TWO legacy inputs, both owned by the wallet and both carrying
    // PSBT_IN_SIGHASH_TYPE = 0x41 (Express/SwapKit response captured in the [REDACTED_TASK_KEY] QA log).
    private val twoInputBchPsbtBase64 = "cHNidP8BAKACAAAAAhV/denIJmpoREOeZLnsiL1s8zY9wlUp7gNxD5lj/BJWAAAAAAD//////HdjnX7eCudJyKImp/SQ7tW3nzs81TWA5QphVqB5elYAAAAAAP////8CoD6KAAAAAAAZdqkUWgv6q0QWiRWkxnfwXrcE8AWiSMWIrD8OAAAAAAAAGXapFKm4ZUqq4dObcSHN9anEFKtbuaIriKwAAAAAAAEA4QIAAAABNUxL+2rbe56cSONMrrQMnuks6enuRGpCIK3B63+3y4MBAAAAakcwRAIgGwynw8iuPk8GlF0FW6GICUoghVLkb8AmQiJwSnWOtNICIDpKly444HUMjDvhewHJvtHEfZr5w3U0Tf48W+OJxzOHQSEC/Cv8fuuV+PoS+GTxXJlQqGq7CNEn2/vVAhrHZh6f1VP/////AukjigAAAAAAGXapFKm4ZUqq4dObcSHN9anEFKtbuaIriKxYnNDzAwAAABl2qRSlAIKSP3O7pyhKi7/Vob6ZHtmym4isAAAAAAEDBEEAAAAAAQDAAQAAAAHtfWe4DsmnUQU1e/HX8oSIe83oHeMz5Jiwk7QPsTgaUQAAAABrSDBFAiEAwvU38RcOmDv30QQ6UQIPLfAXv/Wjs+0yOAV1ndY2naICIBT79TeylT/5lVlYJSHhpKXoDXRAdq+yr/c5IzC+mu4DQSED3HYWETdScVQv0Q23TUBoYijfmy1M4AcnDFi4Rem9+Dr/////AWwqAAAAAAAAGXapFKm4ZUqq4dObcSHN9anEFKtbuaIriKwAAAAAAQMEQQAAAAAAAA=="

    // txFrom of that swap: both inputs' prev P2PKH scripts pay to hash160 a9b8654aaae1d39b7121cdf5a9c414ab5bb9a22b.
    private val twoInputOwnerAddress = "bitcoincash:qz5mse224tsa8xm3y8xlt2wyzj44hwdz9vgr9r08sr"

    // Arbitrary *compressed* pubkey for the wallet fixture (acinq's Psbt.addSignatureToPsbt requires
    // a compressed public key to build fr.acinq.bitcoin.PublicKey). Not used for hash derivation in
    // this test (deriveSignInputs matches on decoded address, not pubkey); only wired through
    // signPsbt -> addSignatureToPsbt, and DeterministicFakeSigner ignores it for the signature value.
    private val ownerPublicKey = PsbtTestFixtures.ownerPublicKey

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

    private fun bchWallet(address: String = ownerAddress): Wallet = Wallet(
        blockchain = Blockchain.BitcoinCash,
        addresses = setOf(Address(address, AddressType.Default)),
        publicKey = Wallet.PublicKey(seedKey = ownerPublicKey, derivationType = null),
        tokens = emptySet(),
    )

    @Test
    fun `deriveSignInputs returns the owner input with FORKID sighash from a real BCH swap PSBT`() {
        // Given
        val provider = PsbtProviderFactory.make(Blockchain.BitcoinCash, bchWallet(), mockk(relaxed = true))

        // When
        val result = provider.deriveSignInputs(realBchPsbtBase64)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val inputs = (result as Result.Success).data
        assertThat(inputs).containsExactly(
            SignInput(address = ownerAddress, index = 0, sighashTypes = listOf(PsbtSighash.ALL_FORKID)),
        )
    }

    @Test
    fun `signPsbt finalizes the BCH input as legacy scriptSig with the 0x41 sighash byte`() = runBlocking {
        // Given
        val wallet = bchWallet()
        val provider = PsbtProviderFactory.make(Blockchain.BitcoinCash, wallet, mockk(relaxed = true))
        val signInputs = (provider.deriveSignInputs(realBchPsbtBase64) as Result.Success).data

        // When
        val signedResult = provider.signPsbt(realBchPsbtBase64, signInputs, DeterministicFakeSigner)

        // Then
        assertThat(signedResult).isInstanceOf(Result.Success::class.java)
        val signedPsbtBase64 = (signedResult as Result.Success).data

        val signedPsbt = when (val decoded = Psbt.read(java.util.Base64.getDecoder().decode(signedPsbtBase64))) {
            is Either.Right -> decoded.value
            is Either.Left -> error("Failed to decode signed BCH PSBT: ${decoded.value}")
        }

        val finalizedInput = signedPsbt.inputs[0]
        assertThat(finalizedInput).isInstanceOf(Input.NonWitnessInput.FinalizedNonWitnessInput::class.java)
        finalizedInput as Input.NonWitnessInput.FinalizedNonWitnessInput

        // scriptSig is PUSH(sig||sighashByte) PUSH(pubkey); the last byte of the first push is the
        // sighash byte the signature was tagged with.
        val sigPush = finalizedInput.scriptSig[0] as OP_PUSHDATA
        val sigBytes = sigPush.data.toByteArray()
        assertThat(sigBytes.last().toInt() and 0xFF).isEqualTo(PsbtSighash.ALL_FORKID)
    }

    @Test
    fun `broadcastPsbt extracts and sends the signed BCH transaction despite its non-Bitcoin sighash byte`() =
        runBlocking {
            // Given
            val networkProvider = mockk<BitcoinNetworkProvider>()
            val sentRawTx = slot<String>()
            coEvery { networkProvider.sendTransaction(capture(sentRawTx)) } returns SimpleResult.Success

            val provider = PsbtProviderFactory.make(Blockchain.BitcoinCash, bchWallet(), networkProvider)
            val signInputs = (provider.deriveSignInputs(realBchPsbtBase64) as Result.Success).data
            val signedPsbtBase64 = (provider.signPsbt(realBchPsbtBase64, signInputs, DeterministicFakeSigner)
                as Result.Success).data

            // When
            val result = provider.broadcastPsbt(signedPsbtBase64)

            // Then
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val signedPsbt = readPsbt(signedPsbtBase64)
            val unsignedTx = signedPsbt.global.tx
            val finalizedInput = signedPsbt.inputs[0] as Input.NonWitnessInput.FinalizedNonWitnessInput

            val broadcastTx = Transaction.read(sentRawTx.captured)
            assertThat(broadcastTx.version).isEqualTo(unsignedTx.version)
            assertThat(broadcastTx.lockTime).isEqualTo(unsignedTx.lockTime)
            assertThat(broadcastTx.txOut).isEqualTo(unsignedTx.txOut)
            assertThat(broadcastTx.txIn.map { it.outPoint }).isEqualTo(unsignedTx.txIn.map { it.outPoint })
            assertThat(broadcastTx.txIn.single().signatureScript)
                .isEqualTo(ByteVector(Script.write(finalizedInput.scriptSig)))

            // BCH has no SegWit: the broadcast bytes must be the legacy serialization (no 0x0001 marker),
            // and the returned hash must be the txid of exactly those bytes.
            assertThat(broadcastTx.txIn.single().witness.isNull()).isTrue()
            assertThat(sentRawTx.captured).isEqualTo(Transaction.write(broadcastTx).toHexString())
            assertThat((result as Result.Success).data).isEqualTo(broadcastTx.txid.value.toHex())
        }

    @Test
    fun `broadcastPsbt signs and extracts every owned input of a two-input BCH swap PSBT`() = runBlocking {
        // Given
        val networkProvider = mockk<BitcoinNetworkProvider>()
        val sentRawTx = slot<String>()
        coEvery { networkProvider.sendTransaction(capture(sentRawTx)) } returns SimpleResult.Success

        val wallet = bchWallet(address = twoInputOwnerAddress)
        val provider = PsbtProviderFactory.make(Blockchain.BitcoinCash, wallet, networkProvider)
        val signInputs = (provider.deriveSignInputs(twoInputBchPsbtBase64) as Result.Success).data

        // When
        val signedPsbtBase64 = (provider.signPsbt(twoInputBchPsbtBase64, signInputs, DeterministicFakeSigner)
            as Result.Success).data
        val result = provider.broadcastPsbt(signedPsbtBase64)

        // Then
        assertThat(signInputs.map { it.index }).containsExactly(0, 1).inOrder()
        assertThat(signInputs.map { it.sighashTypes })
            .containsExactly(listOf(PsbtSighash.ALL_FORKID), listOf(PsbtSighash.ALL_FORKID))
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val signedPsbt = readPsbt(signedPsbtBase64)
        val expectedScriptSigs = signedPsbt.inputs.map {
            ByteVector(Script.write((it as Input.NonWitnessInput.FinalizedNonWitnessInput).scriptSig))
        }
        val broadcastTx = Transaction.read(sentRawTx.captured)
        assertThat(broadcastTx.txIn.map { it.signatureScript }).isEqualTo(expectedScriptSigs)
        assertThat(broadcastTx.txOut).isEqualTo(signedPsbt.global.tx.txOut)
    }

    /**
     * Pins WHY Bitcoin Cash needs its own extractor ([REDACTED_TASK_KEY]): acinq's [Psbt.extract] validates the
     * assembled transaction with `Transaction.correctlySpends(..., STANDARD_SCRIPT_VERIFY_FLAGS)`, and
     * `SCRIPT_VERIFY_STRICTENC` rejects BCH's 0x41 (SIGHASH_ALL|FORKID) as an undefined hash type — so a
     * correctly signed BCH transaction can never be extracted through it. If a bitcoin-kmp bump ever makes
     * this succeed, this test fails and the bespoke extractor can be reconsidered.
     */
    @Test
    fun `acinq extract rejects the signed BCH transaction because 0x41 is not a Bitcoin sighash type`() = runBlocking {
        // Given
        val provider = PsbtProviderFactory.make(Blockchain.BitcoinCash, bchWallet(), mockk(relaxed = true))
        val signInputs = (provider.deriveSignInputs(realBchPsbtBase64) as Result.Success).data
        val signedPsbtBase64 = (provider.signPsbt(realBchPsbtBase64, signInputs, DeterministicFakeSigner)
            as Result.Success).data

        // When
        val extracted = readPsbt(signedPsbtBase64).extract()

        // Then
        assertThat(extracted).isInstanceOf(Either.Left::class.java)
        assertThat((extracted as Either.Left).value).isEqualTo(
            UpdateFailure.CannotExtractTx("extracted transaction doesn't pass standard script validation"),
        )
    }

    private fun readPsbt(base64: String): Psbt {
        return when (val decoded = Psbt.read(java.util.Base64.getDecoder().decode(base64))) {
            is Either.Right -> decoded.value
            is Either.Left -> error("Failed to decode BCH PSBT: ${decoded.value}")
        }
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