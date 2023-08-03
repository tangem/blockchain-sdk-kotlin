package com.tangem.blockchain.blockchains.near

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class NearWalletInfo(
    val amount: BigDecimal,
    val blockHash: String,
    val blockHeight: Long,
)

data class NearGasPrice(
    val gasPrice: BigDecimal,
    val blockHeight: Long,
)

data class SendTransactionResult(
    val hash: String,
)