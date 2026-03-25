package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.decodeBase64
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either

/**
 * Serializer for Bitcoin PSBT (Partially Signed Bitcoin Transaction) format.
 *
 * Handles bidirectional conversion between PSBT objects and Base64-encoded strings,
 * which is the standard format for transmitting PSBTs in Bitcoin applications.
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0174.mediawiki">BIP-174 PSBT</a>
 */
internal object PsbtSerializer {

    /**
     * Parses PSBT from Base64 encoding.
     *
     * @param psbtBase64 Base64-encoded PSBT string
     * @return Success with parsed Psbt object, or Failure with error
     */
    fun parsePsbt(psbtBase64: String): Result<Psbt> {
        val psbtBytes = decodePsbtBase64(psbtBase64).successOr { return it }
        return readPsbt(psbtBytes)
    }

    /**
     * Serializes PSBT to Base64 encoding.
     *
     * @param psbt PSBT object to serialize
     * @return Success with Base64-encoded string, or Failure with error
     */
    fun serializePsbt(psbt: Psbt): Result<String> {
        return try {
            val psbtBytes = Psbt.write(psbt)
            Result.Success(psbtBytes.toByteArray().encodeBase64NoWrap())
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to serialize PSBT: ${e.message}"),
            )
        }
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
}