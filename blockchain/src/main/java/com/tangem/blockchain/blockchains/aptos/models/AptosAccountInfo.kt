package com.tangem.blockchain.blockchains.aptos.models

import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import java.math.BigDecimal

internal data class AptosAccountInfo(
    val sequenceNumber: Long,
    val balance: BigDecimal,
    val tokens: List<AptosResource.TokenResource>,
)
