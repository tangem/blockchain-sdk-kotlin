package com.tangem.blockchain.common.derivation

enum class DerivationStyle {

    @Deprecated("Will be removed after refactoring")
    LEGACY,

    @Deprecated("Will be removed after refactoring")
    NEW,

    /**
     * All have derivation according to BIP44 `coinType`
     * https://github.com/satoshilabs/slips/blob/master/slip-0044.md
     */
    V1,

    /**
     * `EVM-like` have identical derivation with `ethereumCoinType == 60`
     *  Other blockchains - according to BIP44 `coinType`
     */
    V2,

    /**
     * `EVM-like` blockchains have identical derivation with `ethereumCoinType == 60`
     *  `Bitcoin-like` blockchains have different derivation related to `BIP`. For example `Legacy` and `SegWit`
     */
    V3,

    ;

    fun getConfig(): DerivationConfig {
        return when (this) {
            LEGACY, V1 -> DerivationConfigV1
            NEW, V2 -> DerivationConfigV2
            V3 -> DerivationConfigV3
        }
    }
}
