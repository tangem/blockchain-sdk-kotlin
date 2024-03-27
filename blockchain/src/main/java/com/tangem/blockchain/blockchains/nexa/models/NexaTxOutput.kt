package com.tangem.blockchain.blockchains.nexa.models

import org.bitcoinj.script.Script

data class NexaTxOutput(
    val satoshiAmount: Long,
    val script: Script,
    val type: Type,
) {
    enum class Type(val byte: Byte) {
        SATOSCRIPT(0),
        TEMPLATE(1);
    }
}