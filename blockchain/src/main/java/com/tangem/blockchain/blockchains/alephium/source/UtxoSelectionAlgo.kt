package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.common.transaction.getMinimumRequiredUTXOsToSend
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr

/**
 * https://github.com/alephium/alephium/blob/master/flow/src/main/scala/org/alephium/flow/core/UtxoSelectionAlgo.scala
 * We sort the Utxos based on the amount and type
 *   - the Utxos with higher persisted level are selected first (confirmed Utxos are of high priority)
 *   - the Utxos with smaller amounts are selected first
 *   - alph selection non-token Utxos first
 *   - the above logic applies to both ALPH and tokens.
 */
// scalastyle:off parameter.number
internal object UtxoSelectionAlgo {

    data class Selected(val assets: List<AssetOutputInfo>, val gas: GasBox)

    data class ProvidedGas(
        val gasOpt: GasBox,
        val gasPrice: GasPrice,
        val gasEstimationMultiplier: GasEstimationMultiplier?,
    )

    data class AssetAmounts(val alph: U256, val tokens: List<Pair<TokenId, U256>>)

    data class Build(val providedGas: ProvidedGas) {

        fun select(amounts: AssetAmounts, utxos: List<AssetOutputInfo>): Result<Selected> {
            val gasPrice = providedGas.gasPrice
            val gas = providedGas.gasOpt
            val transactionFeeAmount = (gasPrice * gas).v.toBigDecimal()
            return getMinimumRequiredUTXOsToSend(
                unspentOutputs = utxos,
                transactionAmount = amounts.alph.v.toBigDecimal(),
                transactionFeeAmount = transactionFeeAmount,
                dustValue = dustUtxoAmount.v.toBigDecimal(),
                unspentToAmount = { it.output.amount.v.toBigDecimal() },
            )
                .map { selected -> Result.success(Selected(selected, gas)) }
                .successOr { return Result.failure(it.error) }
        }
    }
}