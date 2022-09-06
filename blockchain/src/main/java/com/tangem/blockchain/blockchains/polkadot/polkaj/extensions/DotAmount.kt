package com.tangem.blockchain.blockchains.polkadot.polkaj.extensions

import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.DotAmount
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
fun DotAmount.toBigDecimal(network: SS58Type.Network): BigDecimal {
    val decimals = network.amountUnits.main.decimals
    return if (decimals == 0) {
        value.toBigDecimal()
    } else {
        value.toBigDecimal().movePointLeft(decimals)
    }
}

internal fun BigDecimal.toDot(network: SS58Type.Network): DotAmount {
    return DotAmount.from(this.toDouble(), network.amountUnits)
}