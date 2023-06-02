package com.tangem.blockchain.common.transaction

import com.tangem.blockchain.common.Amount

data class Fee(
    val amount: Amount,
    val extras: FeeExtras = EmptyFeeExtras
)
