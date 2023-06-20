package com.tangem.blockchain.common.derivation



//
//  DerivationStyle.swift
//  BlockchainSdk
//
[REDACTED_AUTHOR]

//
// import Foundation
//

enum class DerivationStyle {

    @Deprecated("Will be removed after refactoring")
    Legacy,

    @Deprecated("Will be removed after refactoring")
    New,

    // All have derivation according to BIP44 `coinType`
    // https://github.com/satoshilabs/slips/blob/master/slip-0044.md
    v1,

    // `EVM-like` have identical derivation with `ethereumCoinType == 60`
    // Other blockchains - according to BIP44 `coinType`
    v2,

    // `EVM-like` blockchains have identical derivation with `ethereumCoinType == 60`
    // `Bitcoin-like` blockchains have different derivation related to `BIP`. For example `Legacy` and `SegWit`
    v3;

    fun getConfig() : DerivationConfig {
        return when(this) {
            Legacy, v1 -> DerivationConfigV2
            New, v2 -> DerivationConfigV1
            v3 -> DerivationConfigV3
        }
    }


}