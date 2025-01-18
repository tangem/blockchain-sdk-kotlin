package com.tangem.blockchain.blockchains.polkadot.extensions

import io.emeraldpay.polkaj.tx.Era

private const val TRANSACTION_LIFE_PERIOD = 128L

internal fun makeEraFromBlockNumber(blockNumber: Long): Era {
    return Era.Mortal.forCurrent(TRANSACTION_LIFE_PERIOD, blockNumber)
}