package com.tangem.blockchain.blockchains.ton.models

import com.tangem.blockchain.blockchains.ton.network.TonAccountState
import com.tangem.blockchain.common.Token
import java.math.BigDecimal

data class TonWalletInfo(
    val accountState: TonAccountState,
    val balance: BigDecimal,
    val sequenceNumber: Int,
    val jettonDatas: Map<Token, JettonData>,
)

data class JettonData(
    val balance: BigDecimal,
    val jettonWalletAddress: String,
)