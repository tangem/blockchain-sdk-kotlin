package com.tangem.blockchain.blockchains.alephium.source

internal data class GasPrice(val value: U256) : Comparable<GasPrice> {
    // this is safe as value <= ALPH.MaxALPHValue
    operator fun times(gas: GasBox): U256 {
        return value.mulUnsafe(gas.toU256())
    }

    override fun compareTo(other: GasPrice): Int {
        return this.value.compareTo(other.value)
    }
}