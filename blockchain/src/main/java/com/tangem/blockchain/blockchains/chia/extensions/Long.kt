package com.tangem.blockchain.blockchains.chia.extensions

internal fun Long.chiaEncode() = this.toBigInteger().toByteArray()
