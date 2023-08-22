package com.tangem.blockchain.extensions

import com.tangem.blockchain.common.Blockchain
import wallet.core.jni.CoinType

/**
 * Converts tangem Blockchain object to WalletCore CoinType
 */
internal val Blockchain.walletCoreWalletType: CoinType
    get() = when (this) {
        Blockchain.Cosmos, Blockchain.CosmosTestnet -> CoinType.COSMOS
        Blockchain.TON, Blockchain.TONTestnet -> CoinType.TON
        Blockchain.TerraV1 -> CoinType.TERRA
        Blockchain.TerraV2 -> CoinType.TERRAV2
        Blockchain.Cardano -> CoinType.CARDANO
        else -> throw IllegalStateException()
    }
