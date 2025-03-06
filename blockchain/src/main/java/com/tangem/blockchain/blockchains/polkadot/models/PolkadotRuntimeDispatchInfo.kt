package com.tangem.blockchain.blockchains.polkadot.models

import java.math.BigInteger

internal class PolkadotRuntimeDispatchInfo(
    val refTime: Int,
    val proofSize: Int,
    val classType: Byte,
    val partialFee: BigInteger,
)