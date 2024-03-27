package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressDecodedParts
import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import com.tangem.blockchain.blockchains.nexa.models.NexaTxOutput
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptChunk
import org.bitcoinj.script.ScriptOpCodes

fun NexaAddressDecodedParts.outputScript() : Script {
    //SatoshiScript(chainSelector, Type.SATOSCRIPT, OP.DUP, OP.HASH160, OP.push(rawAddr), OP.EQUALVERIFY, OP.CHECKSIG)
    //SatoshiScript(blockchain, SatoshiScript.Type.TEMPLATE, BCHserialized(data, SerializationType.NETWORK).deByteArray())

    val script = when (addressType) {
        NexaAddressType.P2PKH -> ScriptBuilder()
            .addChunk(ScriptChunk(ScriptOpCodes.OP_DUP, null))
            .addChunk(ScriptChunk(ScriptOpCodes.OP_HASH160, null))
            .data(hash)
            .addChunk(ScriptChunk(ScriptOpCodes.OP_EQUALVERIFY, null))
            .addChunk(ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null))
            .build()
        NexaAddressType.TEMPLATE -> {
            Script(hash.readVarintBuf())
        }
        else -> error("unsupported")
    }

    return script
}
fun NexaAddressDecodedParts.outputType() : NexaTxOutput.Type {
    return when(addressType) {
        NexaAddressType.TEMPLATE -> NexaTxOutput.Type.TEMPLATE
        else -> NexaTxOutput.Type.SATOSCRIPT
    }
}


private fun ByteArray.readVarintBuf() : ByteArray {
    val first = this.getOrNull(0) ?: return this
    return when(first.toInt()) {
        0xFD -> this.copyOfRange(1 + 2, this.size)
        0xFE -> this.copyOfRange(1 + 4, this.size)
        0xFF -> this.copyOfRange(1 + 8, this.size)
        else -> this.copyOfRange(1, this.size)
    }
}