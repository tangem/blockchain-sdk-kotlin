package com.tangem.blockchain.blockchains.bitcoincash.cashaddr

internal enum class BitcoinCashLikeAddressPrefix(val addressPrefix: String) {
    BitcoinCash("bitcoincash"),
    BitcoinCashTestnet("bchtest"),
    Nexa("nexa"),
    NexaTestnet("nexatest"),
}