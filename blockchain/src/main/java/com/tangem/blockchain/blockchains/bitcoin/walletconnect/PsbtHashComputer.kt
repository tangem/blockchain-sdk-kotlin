package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.SigVersion
import fr.acinq.bitcoin.Transaction

/**
 * Computes signature hashes for Bitcoin PSBT inputs.
 *
 * Handles both SegWit (BIP 143) and legacy signature hash computation,
 * determining the appropriate algorithm based on the input type.
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki">BIP-143 SegWit</a>
 */
internal object PsbtHashComputer {

    private const val SIGHASH_ALL = 1
    private const val P2WPKH_SCRIPT_SIZE = 22
    private const val P2WPKH_PREFIX_SIZE = 2
    private const val P2WPKH_KEY_HASH_LENGTH = 20

    /**
     * Computes the signature hash for a given PSBT input.
     *
     * Automatically detects whether to use SegWit (BIP 143) or legacy signature hash
     * computation based on the presence of witnessUtxo or nonWitnessUtxo.
     *
     * @param psbt The PSBT containing the transaction
     * @param inputIndex Index of the input to compute hash for
     * @param sighashType Signature hash type (defaults to SIGHASH_ALL if not specified)
     * @return Success with 32-byte signature hash, or Failure with error
     */
    fun computeHashToSign(
        psbt: fr.acinq.bitcoin.psbt.Psbt,
        inputIndex: Int,
        sighashType: Int = SIGHASH_ALL,
    ): Result<ByteArray> {
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
            signatureVersion = SigVersion.SIGVERSION_WITNESS_V0,
        )
    }

    /**
     * Gets script code for witness signature hash computation.
     */
    private fun getScriptCodeForWitness(
        input: fr.acinq.bitcoin.psbt.Input,
        witnessUtxo: fr.acinq.bitcoin.TxOut,
    ): ByteArray {
        val pubKeyScript = witnessUtxo.publicKeyScript.toByteArray()

        return when {
            input.witnessScript != null -> Script.write(input.witnessScript!!)
            input.redeemScript != null -> Script.write(input.redeemScript!!)
            isP2WpkhScript(pubKeyScript) -> buildP2pkhScriptCode(pubKeyScript)
            else -> pubKeyScript
        }
    }

    /**
     * Builds the P2PKH scriptCode for SegWit v0 P2WPKH inputs (BIP143 requirement).
     */
    private fun buildP2pkhScriptCode(pubKeyScript: ByteArray): ByteArray {
        val keyHash = pubKeyScript.copyOfRange(P2WPKH_PREFIX_SIZE, pubKeyScript.size)
        val script = Script.pay2pkh(keyHash)
        return Script.write(script)
    }

    /**
     * Detects a simple P2WPKH scriptPubKey (0 <20-byte-pubkey-hash>).
     */
    private fun isP2WpkhScript(pubKeyScript: ByteArray): Boolean {
        return pubKeyScript.size == P2WPKH_SCRIPT_SIZE &&
            pubKeyScript[0] == 0x00.toByte() &&
            pubKeyScript[1] == P2WPKH_KEY_HASH_LENGTH.toByte()
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
     * Extension to handle Result unwrapping.
     */
    private inline fun <T> Result<T>.successOr(onFailure: (Result.Failure) -> Nothing): T {
        return when (this) {
            is Result.Success -> this.data
            is Result.Failure -> onFailure(this)
        }
    }
}