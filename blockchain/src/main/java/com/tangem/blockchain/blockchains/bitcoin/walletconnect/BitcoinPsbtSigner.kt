package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.psbt.PsbtSighashStrategy
import com.tangem.blockchain.common.psbt.PsbtTransactionExtractor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Handler for Bitcoin PSBT (Partially Signed Bitcoin Transaction) operations.
 *
 * Implements BIP 174 PSBT signing and serialization using ACINQ bitcoin-kmp library.
 *
 * @property wallet The Bitcoin wallet instance
 * @property networkProvider Network provider for broadcasting transactions
 * @property sighashStrategy Chain-specific signature-hash computation
 * @property transactionExtractor Chain-specific assembly of the final transaction from a finalized PSBT
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0174.mediawiki">BIP-174 PSBT</a>
 * @see <a href="https://github.com/ACINQ/bitcoin-kmp">ACINQ bitcoin-kmp library</a>
 */
internal class BitcoinPsbtSigner(
    private val wallet: Wallet,
    private val networkProvider: BitcoinNetworkProvider,
    private val sighashStrategy: PsbtSighashStrategy,
    private val transactionExtractor: PsbtTransactionExtractor,
) {

    private val psbtSerializer = PsbtSerializer
    private val signatureApplier = PsbtSignatureApplier()

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
            psbt = psbt,
            signatures = signatures,
            signInputs = signInputs,
            inputIndices = inputIndices,
            publicKey = wallet.publicKey.blockchainKey,
            sighashByte = sighashStrategy.sighashByte,
        ).successOr { return it }
        val finalizedPsbt = signatureApplier.finalizePsbt(signedPsbt, inputIndices)

        return psbtSerializer.serializePsbt(finalizedPsbt)
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

            val inputIndex = signInput.index

            if (inputIndex >= psbt.inputs.size) {
                return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "Input index $inputIndex out of bounds (max ${psbt.inputs.size - 1})",
                    ),
                )
            }

            val sighashType = signInput.sighashTypes?.firstOrNull() ?: sighashStrategy.sighashByte
            val hash = sighashStrategy.computeHashToSign(psbt, inputIndex, sighashType).successOr { return it }
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
        val transaction = transactionExtractor.extract(psbt).successOr { return it }

        val rawTx = Transaction.write(transaction).toHexString()
        return when (val result = networkProvider.sendTransaction(rawTx)) {
            is SimpleResult.Success -> Result.Success(transaction.txid.value.toHex())
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
}