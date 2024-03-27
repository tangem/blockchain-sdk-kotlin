package com.tangem.blockchain.blockchains.nexa.models

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptChunk
import org.bitcoinj.script.ScriptOpCodes
import java.math.BigDecimal

data class NexaUnspentOutput(
    val amountSatoshi: Long,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val outpointHash: ByteArray,
    val addressType: NexaAddressType,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NexaUnspentOutput

        if (amountSatoshi != other.amountSatoshi) return false
        if (outputIndex != other.outputIndex) return false
        if (!transactionHash.contentEquals(other.transactionHash)) return false
        if (!outpointHash.contentEquals(other.outpointHash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = amountSatoshi.hashCode()
        result = 31 * result + outputIndex.hashCode()
        result = 31 * result + transactionHash.contentHashCode()
        result = 31 * result + outpointHash.contentHashCode()
        return result
    }
}