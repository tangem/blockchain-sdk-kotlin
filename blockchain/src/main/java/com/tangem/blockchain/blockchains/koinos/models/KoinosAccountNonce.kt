package com.tangem.blockchain.blockchains.koinos.models

import java.math.BigInteger

@JvmInline
internal value class KoinosAccountNonce(
    val nonce: BigInteger,
)