package com.tangem.blockchain.extensions

import com.google.common.base.CharMatcher

fun Char.isAscii(): Boolean = CharMatcher.ascii().matches(this)
