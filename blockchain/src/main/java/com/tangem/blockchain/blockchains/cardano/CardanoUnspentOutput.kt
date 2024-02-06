package com.tangem.blockchain.blockchains.cardano

class CardanoUnspentOutput(
    val address: String,
    val amount: Long,
    val outputIndex: Long,
    val transactionHash: ByteArray,
)
