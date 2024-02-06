package com.tangem.blockchain.common

import com.tangem.blockchain.common.Token as BlockchainToken

sealed class FeePaidCurrency {
    object Coin : FeePaidCurrency()
    object SameCurrency : FeePaidCurrency()
    data class Token(val token: BlockchainToken) : FeePaidCurrency()
}
