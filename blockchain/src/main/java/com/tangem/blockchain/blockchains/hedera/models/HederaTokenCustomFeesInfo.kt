package com.tangem.blockchain.blockchains.hedera.models

import java.math.BigDecimal

data class HederaTokenCustomFeesInfo(
    val hasTokenCustomFees: Boolean,
    val additionalHBARFee: BigDecimal,
)