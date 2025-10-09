package com.tangem.blockchain.blockchains.quai

import com.tangem.blockchain.extensions.removeLeadingZero
import com.tangem.common.extensions.hexToBytes
import wallet.core.jni.proto.Ethereum
import java.math.BigInteger

/**
 * Simple utility to convert Ethereum transaction to Protobuf for Quai Network
 */

@Suppress("MagicNumber")
object QuaiProtobufUtils {

    private const val FIELD_1_TYPE = 1
    private const val FIELD_2_TO = 2
    private const val FIELD_3_NONCE = 3
    private const val FIELD_4_VALUE = 4
    private const val FIELD_5_GAS = 5
    private const val FIELD_6_DATA = 6
    private const val FIELD_7_CHAIN_ID = 7
    private const val FIELD_8_GAS_PRICE = 8
    private const val FIELD_9_ACCESS_LIST = 9
    private const val FIELD_10_ACCESS_V = 10
    private const val FIELD_11_ACCESS_R = 11
    private const val FIELD_12_ACCESS_S = 12

    /**
     * Converts Ethereum SigningInput to Protobuf format for Quai Network
     * Creates a Protobuf message with all required fields for Quai
     */
    fun buildUnsignedProto(signingInput: Ethereum.SigningInput): ByteArray {
        return buildCommonTransactionFields(signingInput).toByteArray()
    }

    /**
     * Signed variant with V/R/S
     */
    fun convertSigningInputToProtobuf(
        signingInput: Ethereum.SigningInput,
        vSignature: ByteArray, // must be 1 byte: 0x00 or 0x01 (yParity)
        rSignature32: ByteArray, // must be 32 bytes
        sSignature32: ByteArray, // must be 32 bytes
    ): ByteArray {
        return buildList {
            addAll(buildCommonTransactionFields(signingInput))

            // "V" (field 10) - bytes (yParity: 0x00 or 0x01)
            addAll(encodeBytes(FIELD_10_ACCESS_V, vSignature))

            // "R" (field 11) - bytes (signature 32 bytes)
            addAll(encodeBytes(FIELD_11_ACCESS_R, rSignature32))

            // "S" (field 12) - bytes (signature 32 bytes)
            addAll(encodeBytes(FIELD_12_ACCESS_S, sSignature32))
        }.toByteArray()
    }

    private fun buildCommonTransactionFields(signingInput: Ethereum.SigningInput): List<Byte> {
        return buildList {
            // "Type" (field 1) - uint64 (QuaiTxType = 0)
            addAll(encodeVarInt(FIELD_1_TYPE, 0L))

            // "To" (field 2) - bytes (20 bytes address from hex)
            val toAddress = signingInput.toAddress.hexToBytes()
            addAll(encodeBytes(FIELD_2_TO, toAddress))

            // "Nonce" (field 3) - uint64
            val nonceValue = BigInteger(signingInput.nonce.toByteArray()).toLong()
            addAll(encodeVarInt(FIELD_3_NONCE, nonceValue))

            // "Value" (field 4) - bytes (unsigned, no leading 0x00s)
            val valueBytes = stripLeadingZeros(signingInput.transaction.contractGeneric.amount.toByteArray())
            addAll(encodeBytes(FIELD_4_VALUE, valueBytes))

            // "Gas" (field 5) - uint64
            val gasValue = BigInteger(signingInput.gasLimit.toByteArray()).toLong()
            addAll(encodeVarInt(FIELD_5_GAS, gasValue))

            // "Data" (field 6) - bytes
            addAll(encodeBytes(FIELD_6_DATA, signingInput.transaction.contractGeneric.data.toByteArray()))

            // "ChainId" (field 7) - bytes (unsigned, minimal)
            val chainIdBytes = stripLeadingZeros(signingInput.chainId.toByteArray())
            addAll(encodeBytes(FIELD_7_CHAIN_ID, chainIdBytes))

            // "GasPrice" (field 8) - bytes (unsigned, minimal)
            val gasPriceBytesSigned = signingInput.gasPrice.toByteArray().removeLeadingZero()
            addAll(encodeBytes(FIELD_8_GAS_PRICE, gasPriceBytesSigned))

            // "AccessList" (field 9) - empty for now
            addAll(encodeBytes(FIELD_9_ACCESS_LIST, ByteArray(0)))
        }
    }

    private fun encodeVarInt(fieldNumber: Int, value: Long): List<Byte> {
        val tag = fieldNumber shl 3 or 0
        return buildList {
            addAll(encodeVarInt(tag.toLong()))
            addAll(encodeVarInt(value))
        }
    }

    private fun encodeBytes(fieldNumber: Int, value: ByteArray): List<Byte> {
        val tag = fieldNumber shl 3 or 2
        return buildList {
            addAll(encodeVarInt(tag.toLong()))
            addAll(encodeVarInt(value.size.toLong()))
            addAll(value.toList())
        }
    }

    private fun encodeVarInt(value: Long): List<Byte> {
        return buildList {
            var v = value
            while (v >= 0x80) {
                add((v or 0x80).toByte())
                v = v ushr 7
            }
            add(v.toByte())
        }
    }

    private fun stripLeadingZeros(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return bytes
        var start = 0
        if (bytes[0].toInt() == 0) {
            while (start < bytes.size - 1 && bytes[start].toInt() == 0) start++
        }
        return bytes.copyOfRange(start, bytes.size)
    }
}