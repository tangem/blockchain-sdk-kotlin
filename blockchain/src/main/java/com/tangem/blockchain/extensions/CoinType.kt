package com.tangem.blockchain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toDecompressedPublicKey
import wallet.core.jni.CoinType
import wallet.core.jni.PublicKeyType

/**
 * Converts tangem Blockchain object to TrustWallet CoinType
 */
internal val Blockchain.trustWalletCoinType: CoinType
    get() = when (this) {
        Blockchain.Cosmos, Blockchain.CosmosTestnet -> CoinType.COSMOS
        Blockchain.TON, Blockchain.TONTestnet -> CoinType.TON
        Blockchain.TerraV1 -> CoinType.TERRA
        Blockchain.TerraV2 -> CoinType.TERRAV2
        Blockchain.Near, Blockchain.NearTestnet -> CoinType.NEAR
        Blockchain.Cardano -> CoinType.CARDANO
        Blockchain.VeChain, Blockchain.VeChainTestnet -> CoinType.VECHAIN
        Blockchain.Aptos, Blockchain.AptosTestnet -> CoinType.APTOS
        Blockchain.Algorand, Blockchain.AlgorandTestnet -> CoinType.ALGORAND
        Blockchain.Filecoin -> CoinType.FILECOIN
        Blockchain.Sei, Blockchain.SeiTestnet -> CoinType.SEI
        else -> error("Unsupported blockchain: $this")
    }

internal fun CoinType.preparePublicKeyByType(data: ByteArray): ByteArray {
    return when (publicKeyType()) {
        PublicKeyType.SECP256K1 -> data.toCompressedPublicKey()
        PublicKeyType.SECP256K1EXTENDED -> data.toDecompressedPublicKey()
        else -> data
    }
}