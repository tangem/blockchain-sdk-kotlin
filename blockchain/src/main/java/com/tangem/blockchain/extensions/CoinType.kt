package com.tangem.blockchain.extensions

import com.tangem.blockchain.common.Blockchain
import wallet.core.jni.CoinType

/**
 * Converts tangem Blockchain object to TrustWallet CoinType
 */
internal val Blockchain.trustWalletCoinType: CoinType
    get() = when (this) {
        Blockchain.Cosmos, Blockchain.CosmosTestnet -> CoinType.COSMOS
        Blockchain.TON, Blockchain.TONTestnet -> CoinType.TON
        else -> throw IllegalStateException()
    }