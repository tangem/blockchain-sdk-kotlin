package com.tangem.blockchain.common

import com.tangem.blockchain.common.Token as BlockchainSdkToken

sealed class CryptoCurrencyType {
    object Coin : CryptoCurrencyType()
    data class Token(val info: BlockchainSdkToken) : CryptoCurrencyType()
}