package com.tangem.blockchain.blockchains.chia.extensions

import com.tangem.common.extensions.toByteArray

internal fun Long.chiaEncode() = this.toBigInteger().toByteArray()