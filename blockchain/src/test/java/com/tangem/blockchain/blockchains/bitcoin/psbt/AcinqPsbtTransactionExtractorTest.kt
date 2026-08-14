package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OP_PUSHDATA
import fr.acinq.bitcoin.ScriptWitness
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either
import org.junit.Test

/**
 * Guards that the Bitcoin-family extractor keeps acinq's script validation.
 *
 * [REDACTED_TASK_KEY] gave Bitcoin Cash a permissive extractor that skips `Transaction.correctlySpends`. These tests
 * fail if that permissive extractor is ever wired up for Bitcoin/Litecoin/Dogecoin/Dash, where the validation
 * is a genuine safety net: a bad signature must be caught locally, not by the network.
 */
internal class AcinqPsbtTransactionExtractorTest {

    @Test
    fun `extract rejects a witness input finalized with a signature that does not verify`() {
        // Given — finalized with a garbage signature, so the script does not correctly spend the utxo
        val witness = ScriptWitness(
            listOf(ByteVector(ByteArray(71) { 0x01 }), ByteVector(PsbtTestFixtures.ownerPublicKey)),
        )
        val psbt = finalizeWitness(PsbtTestFixtures.singleP2wpkhInputPsbt(), witness)

        // When
        val result = AcinqPsbtTransactionExtractor.extract(psbt)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error.customMessage)
            .contains("extracted transaction doesn't pass standard script validation")
    }

    @Test
    fun `extract rejects a legacy input finalized with a signature that does not verify`() {
        // Given
        val scriptSig = listOf(
            OP_PUSHDATA(ByteVector(ByteArray(71) { 0x01 })),
            OP_PUSHDATA(ByteVector(PsbtTestFixtures.ownerPublicKey)),
        )
        val psbt = when (
            val finalized = PsbtTestFixtures.singleP2pkhLegacyInputPsbt().finalizeNonWitnessInput(0, scriptSig)
        ) {
            is Either.Right -> finalized.value
            is Either.Left -> error("Failed to finalize the legacy fixture: ${finalized.value}")
        }

        // When
        val result = AcinqPsbtTransactionExtractor.extract(psbt)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `extract fails when an input is not finalized at all`() {
        // Given
        val psbt = PsbtTestFixtures.singleP2pkhLegacyInputPsbt()

        // When
        val result = AcinqPsbtTransactionExtractor.extract(psbt)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error.customMessage).contains("some utxos are missing")
    }

    private fun finalizeWitness(psbt: Psbt, witness: ScriptWitness): Psbt =
        when (val finalized = psbt.finalizeWitnessInput(0, witness)) {
            is Either.Right -> finalized.value
            is Either.Left -> error("Failed to finalize the witness fixture: ${finalized.value}")
        }
}