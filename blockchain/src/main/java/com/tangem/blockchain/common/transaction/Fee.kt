package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount
import java.math.BigInteger

sealed class Fee(open val amount: Amount) {

    data class EthereumFee(
        override val amount: Amount,
        val gasLimit: BigInteger,
        val gasPrice: BigInteger,
    ) : Fee(amount)

    data class CommonFee(override val amount: Amount) : Fee(amount)

}
