package com.tangem.blockchain.blockchains.bitcoincash.psbt

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.psbt.PsbtTestFixtures
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.ScriptWitness
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either
import org.junit.Test

/**
 * Structural contract of [BitcoinCashPsbtTransactionExtractor].
 *
 * The BCH extractor deliberately skips the Bitcoin script validation acinq's [Psbt.extract] performs
 * (see [REDACTED_TASK_KEY]), so it must not silently produce a transaction from a PSBT that is not fully finalized —
 * that would broadcast an unspendable transaction instead of failing locally.
 */
internal class BitcoinCashPsbtTransactionExtractorTest {

    @Test
    fun `extract fails when an input is still partially signed rather than finalized`() {
        // Given — a legacy P2PKH input that carries a utxo but no finalScriptSig
        val psbt = PsbtTestFixtures.singleP2pkhLegacyInputPsbt()

        // When
        val result = BitcoinCashPsbtTransactionExtractor.extract(psbt)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat(errorMessage(result)).contains("input #0 is not finalized")
    }

    @Test
    fun `extract rejects a finalized SegWit input because Bitcoin Cash has no SegWit`() {
        // Given — a finalized P2WPKH input: it carries a scriptWitness and no scriptSig
        val witness = ScriptWitness(
            listOf(
                ByteVector(ByteArray(71) { 0x01 }),
                ByteVector(PsbtTestFixtures.ownerPublicKey),
            ),
        )
        val psbt = when (val finalized = PsbtTestFixtures.singleP2wpkhInputPsbt().finalizeWitnessInput(0, witness)) {
            is Either.Right -> finalized.value
            is Either.Left -> error("Failed to finalize the witness fixture: ${finalized.value}")
        }

        // When
        val result = BitcoinCashPsbtTransactionExtractor.extract(psbt)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat(errorMessage(result)).contains("SegWit")
    }

    private fun errorMessage(result: Result<*>): String {
        return (result as Result.Failure).error.customMessage
    }
}