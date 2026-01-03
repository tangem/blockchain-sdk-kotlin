package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount
import java.math.BigDecimal
import java.math.BigInteger

sealed class Fee {

    abstract val amount: Amount

    sealed class Ethereum : Fee() {
        abstract val gasLimit: BigInteger

        data class Legacy(
            override val amount: Amount,
            override val gasLimit: BigInteger,
            val gasPrice: BigInteger,
        ) : Ethereum()

        data class EIP1559(
            override val amount: Amount,
            override val gasLimit: BigInteger,
            val maxFeePerGas: BigInteger,
            val priorityFee: BigInteger,
        ) : Ethereum()

        data class TokenCurrency(
            override val amount: Amount,
            override val gasLimit: BigInteger,
            val tokenPrice: BigDecimal,
            val feeTransferGasLimit: BigInteger,
            val baseGas: BigInteger,
        ) : Ethereum()
    }

    data class Bitcoin(
        override val amount: Amount,
        val satoshiPerByte: BigDecimal,
        val txSize: BigDecimal,
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
        val gasLimit: Long,
    ) : Fee()

    data class CardanoToken(
        override val amount: Amount,
        val minAdaValue: BigDecimal,
    ) : Fee()

    data class Kaspa(
        override val amount: Amount,
        val mass: BigInteger,
        val feeRate: BigInteger,
        val revealTransactionFee: Amount? = null,
    ) : Fee()

    data class Filecoin(
        override val amount: Amount,
        val gasUnitPrice: Long,
        val gasLimit: Long,
        val gasPremium: Long,
    ) : Fee()

    data class Tron(
        override val amount: Amount,
        val remainingEnergy: Long,
        val feeEnergy: Long,
    ) : Fee()

    data class Sui(
        override val amount: Amount,
        val gasBudget: Long,
        val gasPrice: Long,
    ) : Fee()

    data class Alephium(
        override val amount: Amount,
        val gasPrice: BigDecimal,
        val gasAmount: BigDecimal,
    ) : Fee()

    data class Hedera(
        override val amount: Amount,
        // / UI only, this fee must be excluded when building transaction
        val additionalHBARFee: BigDecimal,
    ) : Fee()

    data class Common(override val amount: Amount) : Fee()
}