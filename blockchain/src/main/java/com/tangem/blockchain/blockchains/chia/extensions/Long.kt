package com.tangem.blockchain.blockchains.chia.extensions

import com.tangem.common.extensions.toByteArray

internal fun Long.chiaEncode() = this.toByteArray().dropWhile { it == 0x00.toByte() }.toByteArray()