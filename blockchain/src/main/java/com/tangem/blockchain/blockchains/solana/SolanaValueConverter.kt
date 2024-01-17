package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import java.math.BigDecimal
import java.math.RoundingMode

internal object SolanaValueConverter {

    fun toSol(value: BigDecimal): BigDecimal = value.movePointLeft(Blockchain.Solana.decimals()).toSolanaDecimals()

    fun toSol(value: Long): BigDecimal = toSol(value.toBigDecimal())

    fun toLamports(value: BigDecimal): Long = value.toLamports(Blockchain.Solana.decimals())

    fun toLamports(token: Token, value: BigDecimal): Long = value.toLamports(token.decimals)

    private fun BigDecimal.toSolanaDecimals(): BigDecimal = setScale(Blockchain.Solana.decimals(), RoundingMode.HALF_UP)

    private fun BigDecimal.toLamports(decimals: Int): Long = movePointRight(decimals).toSolanaDecimals().toLong()
}
