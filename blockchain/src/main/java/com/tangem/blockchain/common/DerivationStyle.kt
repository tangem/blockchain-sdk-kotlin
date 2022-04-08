package com.tangem.blockchain.common

enum class DerivationStyle {
    LEGACY, // https://github.com/satoshilabs/slips/blob/master/slip-0044.md
    NEW // All EVM blockchains have identical derivation. For other blockchains it's the same as legacy
}