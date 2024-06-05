package com.tangem.blockchain.blockchains.ton.models

import com.tangem.blockchain.common.Token
import java.math.BigDecimal

data class TonWalletInfo(
    val balance: BigDecimal,
    val sequenceNumber: Int,
    val jettonDatas: Map<Token, JettonData>,
)

data class JettonData(
    val balance: BigDecimal,
    val jettonWalletAddress: String,
)