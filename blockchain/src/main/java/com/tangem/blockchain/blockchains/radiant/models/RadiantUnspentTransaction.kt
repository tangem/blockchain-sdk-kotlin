package com.tangem.blockchain.blockchains.radiant.models

data class RadiantUnspentTransaction(
    val amount: Long,
    val outputIndex: Long,
    val hash: ByteArray,
    val outputScript: ByteArray,
)