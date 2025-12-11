package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.decodeBase64
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either

/**
 * Handler for Bitcoin PSBT (Partially Signed Bitcoin Transaction) operations.
 *
 * Implements BIP 174 PSBT signing and serialization using ACINQ bitcoin-kmp library.
 *
 * @property wallet The Bitcoin wallet instance
 * @property networkProvider Network provider for broadcasting transactions
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0174.mediawiki">BIP-174 PSBT</a>
 * @see <a href="https://github.com/ACINQ/bitcoin-kmp">ACINQ bitcoin-kmp library</a>
 */
internal class BitcoinPsbtSigner(
    private val wallet: Wallet,
    private val networkProvider: BitcoinNetworkProvider,
) {

    /**
     * Signs a PSBT transaction.
     *
     * This method:
     * 1. Parses PSBT from Base64
     * 2. Validates sign inputs against PSBT structure
     * 3. Computes signature hashes for specified inputs
     * 4. Signs hashes using the provided signer
     * 5. Adds signatures to PSBT
     * 6. Returns signed PSBT in Base64
     *
     * @param psbtBase64 PSBT transaction in Base64 encoding
     * @param signInputs List of inputs to sign with address and index
     * @param signer Transaction signer (typically Tangem card)
     * @return Success with signed PSBT in Base64, or Failure with error
     */
    suspend fun signPsbt(psbtBase64: String, signInputs: List<SignInput>, signer: TransactionSigner): Result<String> {
        val psbt = parsePsbt(psbtBase64).successOr { return it }
        validateSignInputs(psbt, signInputs).successOr { return it }

        val (hashesToSign, inputIndices) = prepareSigningData(psbt, signInputs).successOr { return it }
        val signatures = signHashes(hashesToSign, signer).successOr { return it }

        val signedPsbt = applySignatures(psbt, signatures, signInputs, inputIndices).successOr { return it }
        val finalizedPsbt = finalizePsbt(signedPsbt, inputIndices)

        return serializePsbt(finalizedPsbt)
    }

    /**
     * Parses PSBT from Base64 encoding.
     */
    private fun parsePsbt(psbtBase64: String): Result<Psbt> {
        val psbtBytes = decodePsbtBase64(psbtBase64).successOr { return it }
        return readPsbt(psbtBytes)
    }

    /**
     * Decodes PSBT from Base64.
     */
    private fun decodePsbtBase64(psbtBase64: String): Result<ByteArray> {
        return try {
            Result.Success(psbtBase64.decodeBase64())
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to decode PSBT: ${e.message}"),
            )
        }
    }

    /**
     * Reads PSBT from bytes.
     */
    private fun readPsbt(psbtBytes: ByteArray): Result<Psbt> {
        return when (val result = Psbt.read(psbtBytes)) {
            is Either.Right -> Result.Success(result.value)
            is Either.Left -> Result.Failure(
                BlockchainSdkError.CustomError("Failed to parse PSBT: ${result.value}"),
            )
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

    /**
     * Prepares signing data from PSBT and sign inputs.
     */
    private fun prepareSigningData(psbt: Psbt, signInputs: List<SignInput>): Result<Pair<List<ByteArray>, List<Int>>> {
        val hashesToSign = mutableListOf<ByteArray>()
        val inputIndices = mutableListOf<Int>()

        signInputs.forEach { signInput ->
            validateWalletOwnership(signInput.address).successOr { return it }
            validateSighashTypes(signInput.sighashTypes).successOr { return it }

            val sighashType = signInput.sighashTypes?.firstOrNull() ?: SIGHASH_ALL
            val inputIndex = signInput.index

            if (inputIndex >= psbt.inputs.size) {
                return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Input index $inputIndex out of bounds (max ${psbt.inputs.size - 1})",
                    ),
                )
            }

            val hash = computeHashToSign(psbt, inputIndex, sighashType).successOr { return it }
            hashesToSign.add(hash)
            inputIndices.add(inputIndex)
        }

        return Result.Success(hashesToSign to inputIndices)
    }

    /**
     * Validates that address belongs to wallet.
     */
    private fun validateWalletOwnership(address: String): Result<Unit> {
        return if (wallet.addresses.any { it.value == address }) {
            Result.Success(Unit)
        } else {
            Result.Failure(
                BlockchainSdkError.CustomError("Address $address does not belong to this wallet"),
            )
        }
    }

    /**
     * Validates sighash types list.
     */
    private fun validateSighashTypes(sighashTypes: List<Int>?): Result<Unit> {
        return if (sighashTypes != null && sighashTypes.size > 1) {
            Result.Failure(
                BlockchainSdkError.CustomError("Multiple sighash types not supported for single signature"),
            )
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * Signs hashes using the provided signer.
     */
    private suspend fun signHashes(hashesToSign: List<ByteArray>, signer: TransactionSigner): Result<List<ByteArray>> {
        return when (val result = signer.sign(hashesToSign, wallet.publicKey)) {
            is CompletionResult.Success -> Result.Success(result.data)
            is CompletionResult.Failure -> Result.fromTangemSdkError(result.error)
        }
    }

    /**
     * Applies signatures to PSBT.
     */
    private fun applySignatures(
        psbt: Psbt,
        signatures: List<ByteArray>,
        signInputs: List<SignInput>,
        inputIndices: List<Int>,
    ): Result<Psbt> {
        var updatedPsbt = psbt

        signatures.forEachIndexed { index, signature ->
            val inputIndex = inputIndices[index]
            val sighashType = signInputs[index].sighashTypes?.firstOrNull() ?: SIGHASH_ALL
            val derSignature = encodeDerSignature(signature, sighashType.toByte())

            updatedPsbt = addSignatureToPsbt(
                updatedPsbt,
                inputIndex,
                derSignature,
                wallet.publicKey.blockchainKey,
            ).successOr { return it }
        }

        return Result.Success(updatedPsbt)
    }

    /**
     * Finalizes PSBT witness inputs.
     */
    private fun finalizePsbt(psbt: Psbt, inputIndices: List<Int>): Psbt {
        var finalPsbt = psbt

        inputIndices.forEach { index ->
            finalPsbt = finalizeWitnessInput(finalPsbt, index) ?: finalPsbt
        }

        return finalPsbt
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
     * Serializes PSBT to Base64.
     */
    private fun serializePsbt(psbt: Psbt): Result<String> {
        return try {
            val psbtBytes = Psbt.write(psbt)
            Result.Success(psbtBytes.toByteArray().encodeBase64NoWrap())
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to serialize signed PSBT: ${e.message}"),
            )
        }
    }

    /**
     * Broadcasts a finalized PSBT transaction.
     *
     * @param psbt Finalized PSBT
     * @return Success with transaction hash, or Failure with error
     */
    suspend fun broadcastPsbt(psbt: Psbt): Result<String> {
        val transaction = when (val result = psbt.extract()) {
            is Either.Right -> result.value
            is Either.Left -> {
                return Result.Failure(
                    BlockchainSdkError.CustomError("PSBT is not finalized or cannot be extracted: ${result.value}"),
                )
            }
        }

        val rawTx = Transaction.write(transaction).toHexString()
        return when (val result = networkProvider.sendTransaction(rawTx)) {
            is SimpleResult.Success -> Result.Success(transaction.txid.value.reversed().toHex())
            is SimpleResult.Failure -> Result.Failure(result.error)
        }
    }

    /**
     * Validates sign inputs against PSBT.
     */
    private fun validateSignInputs(psbt: Psbt, signInputs: List<SignInput>): Result<Unit> {
        if (signInputs.isEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("No inputs specified for signing"),
            )
        }

        signInputs.forEach { signInput ->
            if (signInput.index < 0 || signInput.index >= psbt.inputs.size) {
                return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Input index ${signInput.index} out of bounds (0..${psbt.inputs.size - 1})",
                    ),
                )
            }
        }

        return Result.Success(Unit)
    }

    /**
     * Computes the hash to sign for a specific input.
     *
     * This method determines whether the input is SegWit or legacy and computes
     * the appropriate signature hash.
     *
     * @param psbt The PSBT containing the input
     * @param inputIndex Index of the input to compute hash for
     * @param sighashType Sighash type flag (default SIGHASH_ALL = 1)
     * @return Success with hash bytes, or Failure with error
     */
    private fun computeHashToSign(psbt: Psbt, inputIndex: Int, sighashType: Int): Result<ByteArray> {
        return try {
            val input = psbt.inputs[inputIndex]
            val tx = psbt.global.tx

            val hash = when {
                input.witnessUtxo != null -> computeWitnessHash(input, tx, inputIndex, sighashType)
                input.nonWitnessUtxo != null -> computeLegacyHash(input, tx, inputIndex, sighashType)
                    .successOr { return it }
                else -> return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Input $inputIndex has neither witnessUtxo nor nonWitnessUtxo",
                    ),
                )
            }

            Result.Success(hash)
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to compute signature hash: ${e.message}"),
            )
        }
    }

    /**
     * Computes BIP 143 signature hash for SegWit inputs.
     */
    private fun computeWitnessHash(
        input: fr.acinq.bitcoin.psbt.Input,
        tx: Transaction,
        inputIndex: Int,
        sighashType: Int,
    ): ByteArray {
        val witnessUtxo = requireNotNull(input.witnessUtxo)
        val scriptCode = getScriptCodeForWitness(input, witnessUtxo)

        return tx.hashForSigning(
            inputIndex,
            scriptCode,
            sighashType,
            witnessUtxo.amount,
            signatureVersion = 0,
        )
    }

    /**
     * Gets script code for witness signature hash computation.
     */
    private fun getScriptCodeForWitness(
        input: fr.acinq.bitcoin.psbt.Input,
        witnessUtxo: fr.acinq.bitcoin.TxOut,
    ): ByteArray {
        return when {
            input.witnessScript != null -> fr.acinq.bitcoin.Script.write(input.witnessScript!!)
            input.redeemScript != null -> fr.acinq.bitcoin.Script.write(input.redeemScript!!)
            else -> witnessUtxo.publicKeyScript.toByteArray()
        }
    }

    /**
     * Computes legacy signature hash for non-SegWit inputs.
     */
    private fun computeLegacyHash(
        input: fr.acinq.bitcoin.psbt.Input,
        tx: Transaction,
        inputIndex: Int,
        sighashType: Int,
    ): Result<ByteArray> {
        val prevTx = requireNotNull(input.nonWitnessUtxo)
        val prevOut = tx.txIn[inputIndex].outPoint
        val prevTxOut = prevTx.txOut.getOrNull(prevOut.index.toInt())
            ?: return Result.Failure(
                BlockchainSdkError.CustomError("Previous output not found at index ${prevOut.index}"),
            )

        val hash = tx.hashForSigning(
            inputIndex,
            prevTxOut.publicKeyScript.toByteArray(),
            sighashType,
        )

        return Result.Success(hash)
    }

    /**
     * Encodes a signature in DER format with sighash type appended.
     *
     * Bitcoin signatures use DER encoding with the sighash type byte appended.
     *
     * @param signature Raw signature bytes (64 bytes: 32 r + 32 s)
     * @param sighashType Sighash type to append
     * @return DER-encoded signature with sighash type
     */
    private fun encodeDerSignature(signature: ByteArray, sighashType: Byte): ByteArray {
        require(signature.size == SIGNATURE_SIZE) {
            "Signature must be $SIGNATURE_SIZE bytes ($R_SIZE r + $S_SIZE s)"
        }

        val (r, s) = extractRandS(signature)
        val derSignature = encodeDer(r, s)

        return derSignature + sighashType
    }

    /**
     * Extracts R and S components from signature.
     */
    private fun extractRandS(signature: ByteArray): Pair<ByteArray, ByteArray> {
        val r = signature.copyOfRange(0, R_SIZE)
        val s = signature.copyOfRange(R_SIZE, SIGNATURE_SIZE)
        return r to s
    }

    /**
     * Encodes r and s values in DER format.
     *
     * DER encoding for ECDSA signatures:
     * - 0x30 [total-length] 0x02 [r-length] [r] 0x02 [s-length] [s]
     *
     * @param r R component of signature
     * @param s S component of signature
     * @return DER-encoded signature
     */
    private fun encodeDer(r: ByteArray, s: ByteArray): ByteArray {
        val rEncoded = encodeIntegerDer(r)
        val sEncoded = encodeIntegerDer(s)
        val content = rEncoded + sEncoded

        return byteArrayOf(DER_SEQUENCE_TAG, content.size.toByte()) + content
    }

    /**
     * Encodes a single integer in DER format.
     */
    private fun encodeIntegerDer(value: ByteArray): ByteArray {
        val trimmed = trimLeadingZeros(value)
        val encoded = addPaddingIfNeeded(trimmed)

        return byteArrayOf(DER_INTEGER_TAG, encoded.size.toByte()) + encoded
    }

    /**
     * Trims leading zeros from byte array.
     */
    private fun trimLeadingZeros(value: ByteArray): ByteArray {
        val trimmed = value.dropWhile { it == 0.toByte() }.toByteArray()
        return if (trimmed.isEmpty()) byteArrayOf(0) else trimmed
    }

    /**
     * Adds padding if high bit is set.
     */
    private fun addPaddingIfNeeded(value: ByteArray): ByteArray {
        val needsPadding = value[0].toInt() and HIGH_BIT_MASK != 0
        return if (needsPadding) byteArrayOf(0) + value else value
    }

    /**
     * Adds a signature to a PSBT input.
     *
     * This creates a new PSBT with the signature added to the partial signatures map.
     *
     * @param psbt The PSBT to update
     * @param inputIndex Index of input to add signature to
     * @param signature DER-encoded signature with sighash type
     * @param publicKey Public key corresponding to the signature
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

    private companion object {
        const val SIGHASH_ALL = 1
        const val SIGNATURE_SIZE = 64
        const val R_SIZE = 32
        const val S_SIZE = 32
        const val DER_SEQUENCE_TAG: Byte = 0x30
        const val DER_INTEGER_TAG: Byte = 0x02
        const val HIGH_BIT_MASK = 0x80
    }
}