package com.tangem.blockchain.blockchains.polkadot.polkaj.extensions

import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.types.DotAmount
import io.emeraldpay.polkaj.types.Units
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
// https://support.polkadot.network/support/solutions/articles/65000168651-what-is-the-existential-deposit-
val SS58Type.Network.existentialDeposit: BigDecimal
    get() = when (this) {
        SS58Type.Network.POLKADOT -> BigDecimal.ONE
        SS58Type.Network.WESTEND -> 0.01.toBigDecimal()
        SS58Type.Network.KUSAMA -> 0.0000333333.toBigDecimal()
        else -> throw UnsupportedOperationException()
    }

val SS58Type.Network.url: String
    get() = when (this) {
        SS58Type.Network.POLKADOT -> "https://rpc.polkadot.io/"
        SS58Type.Network.WESTEND -> "https://westend-rpc.polkadot.io/"
        SS58Type.Network.KUSAMA -> "https://kusama-rpc.polkadot.io/"
        else -> throw UnsupportedOperationException()
    }

val SS58Type.Network.amountUnits: Units
    get() = DotAmount.getUnitsForNetwork(this)

fun SS58Type.Network.isPolkadot(): Boolean {
    return this.value == SS58Type.Network.POLKADOT.value
}

fun SS58Type.Network.isWestend(): Boolean {
    return this.value == SS58Type.Network.KUSAMA.value
}

fun SS58Type.Network.isKusama(): Boolean {
    return this.value == SS58Type.Network.WESTEND.value
}