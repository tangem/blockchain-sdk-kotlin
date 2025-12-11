package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either

/**
 * Applies signatures to Bitcoin PSBT inputs and finalizes witness inputs.
 *
 * Handles the process of adding DER-encoded signatures to PSBT partial signatures map
 * and finalizing witness inputs when all required signatures are present.
 *
 * @property derEncoder Encoder for converting signatures to DER format
 */
internal class PsbtSignatureApplier(
    private val derEncoder: DerSignatureEncoder = DerSignatureEncoder,
) {

    /**
     * Applies signatures to PSBT inputs.
     *
     * @param psbt PSBT to update
     * @param signatures List of raw signatures (64 bytes each)
     * @param signInputs List of sign input specifications
     * @param inputIndices List of input indices corresponding to signatures
     * @param publicKey Public key corresponding to signatures
     * @return Success with updated PSBT, or Failure with error
     */
    fun applySignatures(
        psbt: Psbt,
        signatures: List<ByteArray>,
        signInputs: List<SignInput>,
        inputIndices: List<Int>,
        publicKey: ByteArray,
    ): Result<Psbt> {
        var updatedPsbt = psbt

        signatures.forEachIndexed { index, signature ->
            val inputIndex = inputIndices[index]
            val sighashType = signInputs[index].sighashTypes?.firstOrNull() ?: SIGHASH_ALL
            val derSignature = derEncoder.encodeDerSignature(signature, sighashType.toByte())

            updatedPsbt = addSignatureToPsbt(
                updatedPsbt,
                inputIndex,
                derSignature,
                publicKey,
            ).successOr { return it }
        }

        return Result.Success(updatedPsbt)
    }

    /**
     * Finalizes PSBT witness inputs.
     *
     * Attempts to finalize all specified inputs by constructing witness data
     * from partial signatures.
     *
     * @param psbt PSBT to finalize
     * @param inputIndices List of input indices to finalize
     * @return Finalized PSBT (partially finalized if some inputs can't be finalized)
     */
    fun finalizePsbt(psbt: Psbt, inputIndices: List<Int>): Psbt {
        var finalPsbt = psbt

        inputIndices.forEach { index ->
            finalPsbt = finalizeWitnessInput(finalPsbt, index) ?: finalPsbt
        }

        return finalPsbt
    }

    /**
     * Adds a signature to a PSBT input.
     *
     * @param psbt PSBT to update
     * @param inputIndex Index of input to add signature to
     * @param signature DER-encoded signature with sighash type
     * @param publicKey Public key corresponding to signature
     * @return Success with updated PSBT, or Failure with error
     */
    private fun addSignatureToPsbt(
        psbt: Psbt,
        inputIndex: Int,
        signature: ByteArray,
        publicKey: ByteArray,
    ): Result<Psbt> {
        return try {
            val input = psbt.inputs[inputIndex]
            val pubKey = PublicKey(ByteVector(publicKey))
            val updatedPartialSigs = input.partialSigs + (pubKey to ByteVector(signature))

            val updatedInput = updateInputWithSignature(input, updatedPartialSigs)
                .successOr { return it }

            val updatedPsbt = psbt.copy(
                inputs = psbt.inputs.toMutableList().apply { set(inputIndex, updatedInput) },
            )

            Result.Success(updatedPsbt)
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to add signature to PSBT: ${e.message}"),
            )
        }
    }

    /**
     * Updates PSBT input with new partial signatures.
     */
    private fun updateInputWithSignature(
        input: fr.acinq.bitcoin.psbt.Input,
        partialSigs: Map<PublicKey, ByteVector>,
    ): Result<fr.acinq.bitcoin.psbt.Input> {
        val updatedInput = when (input) {
            is fr.acinq.bitcoin.psbt.Input.WitnessInput.PartiallySignedWitnessInput ->
                input.copy(partialSigs = partialSigs)
            is fr.acinq.bitcoin.psbt.Input.NonWitnessInput.PartiallySignedNonWitnessInput ->
                input.copy(partialSigs = partialSigs)
            else -> return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Unsupported PSBT input type for adding signature: ${input.javaClass.simpleName}",
                ),
            )
        }

        return Result.Success(updatedInput)
    }

    /**
     * Attempts to finalize a single witness input.
     */
    private fun finalizeWitnessInput(psbt: Psbt, index: Int): Psbt? {
        return try {
            val input = psbt.inputs[index]

            when (input) {
                is fr.acinq.bitcoin.psbt.Input.WitnessInput.PartiallySignedWitnessInput -> {
                    if (input.partialSigs.isEmpty()) return null

                    val (pubKey, signature) = input.partialSigs.entries.first()
                    val witness = fr.acinq.bitcoin.ScriptWitness(listOf(signature, pubKey.value))

                    when (val result = psbt.finalizeWitnessInput(index, witness)) {
                        is Either.Right -> result.value
                        is Either.Left -> null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extension to handle Result unwrapping.
     */
    private inline fun <T> Result<T>.successOr(onFailure: (Result.Failure) -> Nothing): T {
        return when (this) {
            is Result.Success -> this.data
            is Result.Failure -> onFailure(this)
        }
    }

    private companion object {
        const val SIGHASH_ALL = 1
    }
}