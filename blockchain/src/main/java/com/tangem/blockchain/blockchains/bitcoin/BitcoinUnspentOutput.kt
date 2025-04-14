package com.tangem.blockchain.blockchains.bitcoin

import java.math.BigDecimal

class BitcoinUnspentOutput(
    val amount: BigDecimal,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val outputScript: ByteArray,
)
