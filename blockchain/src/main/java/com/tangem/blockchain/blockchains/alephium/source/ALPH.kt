package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.U256.Companion.unsafe

internal object ALPH {
    private const val billion: Long = 1000000000L
    private const val quintillion: Long = 1000000000000000000L
    private val CoinInOneALPH: U256 = unsafe(quintillion)
    private val CoinInOneNanoAlph: U256 = unsafe(billion)

    val MaxALPHValue: U256 = unsafe(billion).mulUnsafe(CoinInOneALPH)

    const val MaxTxInputNum: Int = 256
    const val MaxTxOutputNum: Int = 256

    fun nanoAlph(amount: Long): U256 {
        require(amount >= 0)
        return unsafe(amount).mulUnsafe(CoinInOneNanoAlph)
    }
}