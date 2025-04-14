package com.tangem.blockchain.common.trustlines

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain

sealed class AssetRequirementsCondition {

    /**
     * The exact value of the fee for this type of condition is unknown.
     */
    data object PaidTransaction : AssetRequirementsCondition()

    /**
     * The exact value of the fee for this type of condition is stored in `feeAmount`.
     */
    data class PaidTransactionWithFee(
        val blockchain: Blockchain,
        val feeAmount: Amount,
    ) : AssetRequirementsCondition()

    data class IncompleteTransaction(
        val blockchain: Blockchain,
        val amount: Amount,
        val feeAmount: Amount,
    ) : AssetRequirementsCondition()
}