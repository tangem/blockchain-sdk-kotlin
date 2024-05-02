package com.tangem.blockchain.common.trustlines

import com.tangem.blockchain.common.Amount

sealed class AssetRequirementsCondition {

    /**
     * The exact value of the fee for this type of condition is unknown.
     */
    object PaidTransaction : AssetRequirementsCondition()

    /**
     * The exact value of the fee for this type of condition is stored in `feeAmount`.
     */
    data class PaidTransactionWithFee(val feeAmount: Amount) : AssetRequirementsCondition()
}