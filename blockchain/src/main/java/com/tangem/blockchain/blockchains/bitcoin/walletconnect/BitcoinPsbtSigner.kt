package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.extensions.decodeBase64
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
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
    suspend fun signPsbt(
        psbtBase64: String,
        signInputs: List<SignInput>,
        signer: TransactionSigner,
    ): Result<String> {
        // Parse PSBT from Base64
        val psbt = try {
            val psbtBytes = psbtBase64.decodeBase64()
            when (val result = Psbt.read(psbtBytes)) {
                is Either.Right -> result.value
                is Either.Left -> {
                    return Result.Failure(
                        BlockchainSdkError.CustomError("Failed to parse PSBT: ${result.value}"),
                    )
                }
            }
        } catch (e: Exception) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Failed to decode PSBT: ${e.message}"),
            )
        }

        // Validate sign inputs
        val validationResult = validateSignInputs(psbt, signInputs)
        if (validationResult is Result.Failure) {
            return validationResult
        }

        // Build list of hashes to sign
        val hashesToSign = mutableListOf<ByteArray>()
        val inputIndices = mutableListOf<Int>()

        signInputs.forEach { signInput ->
            // Verify address belongs to wallet
            val isValidAddress = wallet.addresses.any { it.value == signInput.address }
            if (!isValidAddress) {
                return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Address ${signInput.address} does not belong to this wallet",
                    ),
                )
            }

            // Get sighash type (default to SIGHASH_ALL = 1)
            val sighashType = signInput.sighashTypes?.firstOrNull() ?: 1

            if (signInput.sighashTypes != null && signInput.sighashTypes.size > 1) {
                return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Multiple sighash types not supported for single signature",
                    ),
                )
            }

            // Extract hash for this input
            try {
                val inputIndex = signInput.index
                if (inputIndex >= psbt.inputs.size) {
                    return Result.Failure(
                        BlockchainSdkError.CustomError(
                            "Input index $inputIndex out of bounds (max ${psbt.inputs.size - 1})",
                        ),
                    )
                }

                // Compute hash to sign for this input
                val hash = computeHashToSign(psbt, inputIndex, sighashType).successOr { return it }

                hashesToSign.add(hash)
                inputIndices.add(inputIndex)
            } catch (e: Exception) {
                return Result.Failure(
                    BlockchainSdkError.CustomError("Failed to compute hash for input ${signInput.index}: ${e.message}"),
                )
            }
        }

        // Sign hashes using the signer
        val signatureResult = signer.sign(hashesToSign, wallet.publicKey)
        val signatures = when (signatureResult) {
            is CompletionResult.Success -> signatureResult.data
            is CompletionResult.Failure -> return Result.fromTangemSdkError(signatureResult.error)
        }

        // Update PSBT with signatures
        var updatedPsbt = psbt
        signatures.forEachIndexed { index, signature ->
            val inputIndex = inputIndices[index]
            val signInput = signInputs[index]
            val sighashType = signInput.sighashTypes?.firstOrNull() ?: 1

            // Create DER-encoded signature with sighash type
            val derSignature = encodeDerSignature(signature, sighashType.toByte())

            // Update PSBT input with partial signature
            updatedPsbt = addSignatureToPsbt(updatedPsbt, inputIndex, derSignature, wallet.publicKey.blockchainKey)
                .successOr { return it }
        }

        var finalPsbt = updatedPsbt
        try {
            for (index in inputIndices) {
                val input = finalPsbt.inputs[index]

                when (input) {
                    is fr.acinq.bitcoin.psbt.Input.WitnessInput.PartiallySignedWitnessInput -> {
                        if (input.partialSigs.isNotEmpty()) {
                            val (pubKey, signature) = input.partialSigs.entries.first()
                            val witness = fr.acinq.bitcoin.ScriptWitness(listOf(signature, pubKey.value))

                            finalPsbt = when (val result = finalPsbt.finalizeWitnessInput(index, witness)) {
                                is Either.Right -> result.value
                                is Either.Left -> {
                                    return Result.Failure(
                                        BlockchainSdkError.CustomError(
                                            "Failed to finalize witness input $index: ${result.value}",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Non-witness or already finalized input, skipping
                    }
                }
            }
        } catch (e: Exception) {
            finalPsbt = updatedPsbt
        }
        val signedPsbtBase64 = try {
            val psbtBytes = Psbt.write(finalPsbt)
            psbtBytes.toByteArray().encodeBase64NoWrap()
        } catch (e: Exception) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Failed to serialize signed PSBT: ${e.message}"),
            )
        }

        return Result.Success(signedPsbtBase64)
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

            // For SegWit inputs (witnessUtxo present)
            val hash = if (input.witnessUtxo != null) {
                // Use witness signature hash (BIP 143)
                val witnessUtxo = input.witnessUtxo!!
                val amount = witnessUtxo.amount

                // Get script code - prefer witness script, fall back to redeem script or witness utxo script
                val scriptCode = when {
                    input.witnessScript != null -> fr.acinq.bitcoin.Script.write(input.witnessScript!!)
                    input.redeemScript != null -> fr.acinq.bitcoin.Script.write(input.redeemScript!!)
                    else -> witnessUtxo.publicKeyScript.toByteArray()
                }

                // Compute BIP 143 signature hash (signatureVersion = 0 for WITNESS_V0)
                tx.hashForSigning(
                    inputIndex,
                    scriptCode,
                    sighashType,
                    amount,
                    signatureVersion = 0,
                )
            } else if (input.nonWitnessUtxo != null) {
                // Use legacy signature hash for non-SegWit inputs
                val prevTx = input.nonWitnessUtxo!!
                val prevOut = tx.txIn[inputIndex].outPoint
                val prevTxOut = prevTx.txOut.getOrNull(prevOut.index.toInt())
                    ?: return Result.Failure(
                        BlockchainSdkError.CustomError(
                            "Previous output not found at index ${prevOut.index}",
                        ),
                    )

                tx.hashForSigning(
                    inputIndex,
                    prevTxOut.publicKeyScript.toByteArray(),
                    sighashType,
                )
            } else {
                return Result.Failure(
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
     * Encodes a signature in DER format with sighash type appended.
     *
     * Bitcoin signatures use DER encoding with the sighash type byte appended.
     *
     * @param signature Raw signature bytes (64 bytes: 32 r + 32 s)
     * @param sighashType Sighash type to append
     * @return DER-encoded signature with sighash type
     */
    private fun encodeDerSignature(signature: ByteArray, sighashType: Byte): ByteArray {
        // Extract r and s from signature (assuming 64 bytes: 32 bytes r + 32 bytes s)
        if (signature.size != 64) {
            throw IllegalArgumentException("Signature must be 64 bytes (32 r + 32 s)")
        }

        val r = signature.copyOfRange(0, 32)
        val s = signature.copyOfRange(32, 64)

        // Encode in DER format
        val derSignature = encodeDer(r, s)

        // Append sighash type
        return derSignature + sighashType
    }

    /**
     * Encodes r and s values in DER format.
     *
     * DER encoding for ECDSA signatures:
     * - 0x30 [total-length] 0x02 [R-length] [R] 0x02 [S-length] [S]
     *
     * @param r R component of signature
     * @param s S component of signature
     * @return DER-encoded signature
     */
    private fun encodeDer(r: ByteArray, s: ByteArray): ByteArray {
        fun encodeInteger(value: ByteArray): ByteArray {
            // Remove leading zeros
            var trimmed = value.dropWhile { it == 0.toByte() }.toByteArray()
            if (trimmed.isEmpty()) trimmed = byteArrayOf(0)

            // Add leading zero if high bit is set (to indicate positive number)
            val needsPadding = (trimmed[0].toInt() and 0x80) != 0
            val encoded = if (needsPadding) {
                byteArrayOf(0) + trimmed
            } else {
                trimmed
            }

            // Tag (0x02 for integer) + length + value
            return byteArrayOf(0x02, encoded.size.toByte()) + encoded
        }

        val rEncoded = encodeInteger(r)
        val sEncoded = encodeInteger(s)
        val content = rEncoded + sEncoded

        // Sequence tag (0x30) + total length + content
        return byteArrayOf(0x30, content.size.toByte()) + content
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

            // Create PublicKey from bytes
            val pubKey = PublicKey(ByteVector(publicKey))

            // Add signature to partial sigs map
            val updatedPartialSigs = input.partialSigs + (pubKey to ByteVector(signature))

            // We need to check the input type and use the appropriate concrete class's copy method
            // since Input is a sealed class and the concrete types have copy methods
            val updatedInput = when (input) {
                is fr.acinq.bitcoin.psbt.Input.WitnessInput.PartiallySignedWitnessInput -> {
                    input.copy(partialSigs = updatedPartialSigs)
                }
                is fr.acinq.bitcoin.psbt.Input.NonWitnessInput.PartiallySignedNonWitnessInput -> {
                    input.copy(partialSigs = updatedPartialSigs)
                }
                else -> {
                    return Result.Failure(
                        BlockchainSdkError.CustomError("Unsupported PSBT input type for adding signature: ${input.javaClass.simpleName}"),
                    )
                }
            }

            // Create new PSBT with updated inputs list
            val updatedInputsList = psbt.inputs.toMutableList()
            updatedInputsList[inputIndex] = updatedInput

            val updatedPsbt = psbt.copy(inputs = updatedInputsList)

            Result.Success(updatedPsbt)
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to add signature to PSBT: ${e.message}"),
            )
        }
    }
}