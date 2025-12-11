package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
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

    private val derEncoder = DerSignatureEncoder
    private val psbtSerializer = PsbtSerializer
    private val hashComputer = PsbtHashComputer
    private val signatureApplier = PsbtSignatureApplier(derEncoder)

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
        val psbt = psbtSerializer.parsePsbt(psbtBase64).successOr { return it }
        validateSignInputs(psbt, signInputs).successOr { return it }

        val (hashesToSign, inputIndices) = prepareSigningData(psbt, signInputs).successOr { return it }
        val signatures = signHashes(hashesToSign, signer).successOr { return it }

        val signedPsbt = signatureApplier.applySignatures(
            psbt,
            signatures,
            signInputs,
            inputIndices,
            wallet.publicKey.blockchainKey,
        ).successOr { return it }
        val finalizedPsbt = signatureApplier.finalizePsbt(signedPsbt, inputIndices)

        return psbtSerializer.serializePsbt(finalizedPsbt)
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

            val hash = hashComputer.computeHashToSign(psbt, inputIndex, sighashType).successOr { return it }
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
    private companion object {
        const val SIGHASH_ALL = 1
    }
}