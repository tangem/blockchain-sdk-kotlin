package com.tangem.blockchain.blockchains.polkadot.polkaj.extensions

import io.emeraldpay.polkaj.scaletypes.AccountInfo
import io.emeraldpay.polkaj.ss58.SS58Type
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
fun AccountInfo.balance(network: SS58Type.Network): BigDecimal {
    return data.free.toBigDecimal(network)
}