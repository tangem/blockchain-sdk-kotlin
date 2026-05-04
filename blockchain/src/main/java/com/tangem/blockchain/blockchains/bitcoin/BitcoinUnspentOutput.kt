package com.tangem.blockchain.blockchains.bitcoin

import java.math.BigDecimal

class BitcoinUnspentOutput(
    val amount: BigDecimal,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val outputScript: ByteArray,
    val address: String? = null,
    val derivationPath: String? = null,
    val publicKey: ByteArray? = null,
)