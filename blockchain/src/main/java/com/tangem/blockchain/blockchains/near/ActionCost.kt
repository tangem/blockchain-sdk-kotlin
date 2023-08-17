package com.tangem.blockchain.blockchains.near

import com.tangem.blockchain.blockchains.near.network.NearAmount
import java.math.BigDecimal

/**
 * Costs of transactions
 * @see <a href="https://docs.near.org/concepts/basics/transactions/gas#the-cost-of-common-actions">Docs</a>
[REDACTED_AUTHOR]
 */
internal sealed class ActionCost(
    val near: NearAmount,
) {

    object CreateAccount : ActionCost(NearAmount(BigDecimal("0.000042")))
    object SendFunds : ActionCost(NearAmount(BigDecimal("0.000045")))
    object AddFullAccessKey : ActionCost(NearAmount(BigDecimal("0.000042")))
    object DeleteKey : ActionCost(NearAmount(BigDecimal("0.000041")))
    object FunctionCall : ActionCost(NearAmount(BigDecimal("0.003")))
    object Stake : ActionCost(NearAmount(BigDecimal("0.00005")))
}