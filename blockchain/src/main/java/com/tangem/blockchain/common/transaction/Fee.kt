package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount
import java.math.BigInteger

sealed class Fee {

    abstract val amount: Amount

    data class Ethereum(
        override val amount: Amount,
        val gasLimit: BigInteger,
        val gasPrice: BigInteger,
    ) : Fee()

    data class Vechain(
        override val amount: Amount,
        val gasPriceCoef: Int,
    ) : Fee()

    data class Aptos(
        override val amount: Amount,
        val gasUnitPrice: Long,
    ) : Fee()

    data class Common(override val amount: Amount) : Fee()
}