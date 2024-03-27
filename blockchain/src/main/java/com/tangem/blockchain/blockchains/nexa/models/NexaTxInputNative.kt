package com.tangem.blockchain.blockchains.nexa.models

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import org.bitcoinj.script.Script

private const val DEFAULT_INPUT_TYPE = 0

data class NexaTxInputNative(
    val output: Output,
    val prevTxId: ByteArray,
    val outputIndex: Long,
    val hashToSign: SignHash,
    val amountSatoshi: Long,
    val addressType: NexaAddressType,
    val sequenceNumber: Int,
) {
    val type: Int
        get() = DEFAULT_INPUT_TYPE

    data class Output(
        val script: Script,
        val valueSatoshi: Long,
    )

    sealed interface SignHash {
        @JvmInline
        value class ReadyForSign(
            val hash: ByteArray,
        ) : SignHash

        @JvmInline
        value class Signed(
            val hash: ByteArray,
        ) : SignHash

        object Empty : SignHash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NexaTxInputNative

        if (output != other.output) return false
        if (!prevTxId.contentEquals(other.prevTxId)) return false
        if (outputIndex != other.outputIndex) return false
        if (hashToSign != other.hashToSign) return false
        if (amountSatoshi != other.amountSatoshi) return false
        if (addressType != other.addressType) return false
        if (sequenceNumber != other.sequenceNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = output.hashCode()
        result = 31 * result + prevTxId.contentHashCode()
        result = 31 * result + outputIndex.hashCode()
        result = 31 * result + hashToSign.hashCode()
        result = 31 * result + amountSatoshi.hashCode()
        result = 31 * result + addressType.hashCode()
        result = 31 * result + sequenceNumber
        return result
    }
}


