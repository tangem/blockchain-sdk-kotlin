package com.tangem.blockchain.blockchains.polkadot.extensions

import io.emeraldpay.polkaj.types.DotAmount
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 04/09/2022.
 */
fun DotAmount.toBigDecimal(decimals: Int): BigDecimal {
    return if (decimals == 0) {
        value.toBigDecimal()
    } else {
        value.toBigDecimal().movePointLeft(decimals)
    }
}
