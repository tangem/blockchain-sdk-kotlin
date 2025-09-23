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
    fun convertSigningInputToProtobuf(
        signingInput: Ethereum.SigningInput,
        vSignature: ByteArray,
        rSignature32: ByteArray,
        sSignature32: ByteArray,
    ): ByteArray {
        return buildList {
            // "Type" (field 1) - uint64 (QuaiTxType = 0)
            addAll(encodeVarInt(FIELD_1_TYPE, 0L))

            // "To" (field 2) - bytes (20 bytes address from hex)
            val toAddress = signingInput.toAddress.removePrefix("0x").hexToBytes()
            addAll(encodeBytes(FIELD_2_TO, toAddress))

            // "Nonce" (field 3) - uint64
            val nonceValue = BigInteger(signingInput.nonce.toByteArray()).toLong()
            addAll(encodeVarInt(FIELD_3_NONCE, nonceValue))

            // "Value" (field 4) - bytes
            addAll(encodeBytes(FIELD_4_VALUE, signingInput.transaction.contractGeneric.amount.toByteArray()))

            // "Gas" (field 5) - uint64
            val gasValue = BigInteger(signingInput.gasLimit.toByteArray()).toLong()
            addAll(encodeVarInt(FIELD_5_GAS, gasValue))

            // "Data" (field 6) - bytes
            addAll(encodeBytes(FIELD_6_DATA, signingInput.transaction.contractGeneric.data.toByteArray()))

            // "ChainId" (field 7) - bytes (from SigningInput)
            val chainIdBytes = signingInput.chainId.toByteArray().removeLeadingZero()
            addAll(encodeBytes(FIELD_7_CHAIN_ID, chainIdBytes))

            // "GasPrice" (field 8) - bytes
            addAll(encodeBytes(FIELD_8_GAS_PRICE, signingInput.gasPrice.toByteArray()))

            // "AccessList" (field 9) - empty for now
            addAll(encodeBytes(FIELD_9_ACCESS_LIST, ByteArray(0)))

            // "V" (field 10) - bytes (signature)
            addAll(encodeBytes(FIELD_10_ACCESS_V, vSignature))

            // "R" (field 11) - bytes (signature 32 bytes)
            addAll(encodeBytes(FIELD_11_ACCESS_R, rSignature32))

            // "S" (field 12) - bytes (signature 32 bytes)
            addAll(encodeBytes(FIELD_12_ACCESS_S, sSignature32))
        }.toByteArray()
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
}