package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount
import java.math.BigDecimal
import java.math.BigInteger

sealed class Fee {

    abstract val amount: Amount

    data class Ethereum(
        override val amount: Amount,
        val gasLimit: BigInteger,
        val gasPrice: BigInteger,
    ) : Fee()

    data class VeChain(
        override val amount: Amount,
        val gasPriceCoef: Int,
        val gasLimit: Long,
    ) : Fee() {
        companion object {
            internal const val MINIMUM_GAS_PRICE_COEFFICIENT = 0
            internal const val NORMAL_GAS_PRICE_COEFFICIENT = 127
            internal const val PRIORITY_GAS_PRICE_COEFFICIENT = 255

            internal const val GAS_TO_VET_DECIMAL = 5
            internal const val NORMAL_FEE_COEFFICIENT = 1.5
            internal const val PRIORITY_FEE_COEFFICIENT = 2.0

            fun getGasPriceCoef(gasLimit: Long, fee: BigDecimal): Int {
                return when (fee.movePointRight(GAS_TO_VET_DECIMAL).toDouble() / gasLimit) {
                    NORMAL_FEE_COEFFICIENT -> NORMAL_GAS_PRICE_COEFFICIENT
                    PRIORITY_FEE_COEFFICIENT -> PRIORITY_GAS_PRICE_COEFFICIENT
                    else -> MINIMUM_GAS_PRICE_COEFFICIENT
                }
            }
        }
    }

    data class Aptos(
        override val amount: Amount,
        val gasUnitPrice: Long,
    ) : Fee()

    data class Common(override val amount: Amount) : Fee()
}
