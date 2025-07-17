package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.common.BlockchainSdkError
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
    interface AssetOrder {
        val byAlph: Comparator<AssetOutputInfo>
        fun byToken(id: TokenId): Comparator<AssetOutputInfo>
    }

    object AssetAscendingOrder : AssetOrder {
        override val byAlph: Comparator<AssetOutputInfo> = Comparator { x, y ->
            val compare1 = x.outputType.cachedLevel.compareTo(y.outputType.cachedLevel)
            if (compare1 != 0) {
                compare1
            } else {
                x.output.amount.compareTo(y.output.amount)
            }
        }

        override fun byToken(id: TokenId): Comparator<AssetOutputInfo> = Comparator { x, y ->
            val compare1 = x.outputType.cachedLevel.compareTo(y.outputType.cachedLevel)

            val tokenX = x.output.tokens.find { it.first.value == id.value }
            val tokenY = y.output.tokens.find { it.first.value == id.value }

            when {
                tokenX != null && tokenY != null -> {
                    if (compare1 != 0) {
                        compare1
                    } else {
                        tokenX.second.compareTo(tokenY.second).let {
                            if (it == 0) byAlph.compare(x, y) else it
                        }
                    }
                }

                tokenX != null -> -1
                tokenY != null -> 1
                else -> byAlph.compare(x, y)
            }
        }
    }

    object AssetDescendingOrder : AssetOrder {
        override val byAlph: Comparator<AssetOutputInfo> = AssetAscendingOrder.byAlph.reversed()
        override fun byToken(id: TokenId): Comparator<AssetOutputInfo> = AssetAscendingOrder.byToken(id).reversed()
    }

    data class Selected(val assets: List<AssetOutputInfo>, val gas: GasBox)

    data class SelectedSoFar(val alph: U256, val selected: List<AssetOutputInfo>, val rest: List<AssetOutputInfo>)

    data class ProvidedGas(
        val gasOpt: GasBox,
        val gasPrice: GasPrice,
        val gasEstimationMultiplier: GasEstimationMultiplier?,
    )

    data class AssetAmounts(val alph: U256, val tokens: List<Pair<TokenId, U256>>)

    data class TxInputWithAsset(val input: TxInput, val asset: AssetOutputInfo) {
        companion object {
            fun from(asset: AssetOutputInfo, unlockScript: UnlockScript): TxInputWithAsset {
                return TxInputWithAsset(TxInput(asset.ref, unlockScript), asset)
            }
        }
    }

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

    data class BuildWithOrder(
        val providedGas: ProvidedGas,
        val assetOrder: AssetOrder,
    ) {
        fun select(amounts: AssetAmounts, utxos: List<AssetOutputInfo>): Result<Selected> {
            val gasPrice = providedGas.gasPrice
            val gas = providedGas.gasOpt
            val amountsWithGas = amounts.copy(alph = amounts.alph.addUnsafe(gasPrice * gas))
            return SelectionWithoutGasEstimation(assetOrder).select(amountsWithGas, utxos).map { selectedSoFar ->
                Selected(selectedSoFar.selected, gas)
            }
        }
    }

    data class SelectionWithoutGasEstimation(val assetOrder: AssetOrder) {
        fun select(amounts: AssetAmounts, allUtxos: List<AssetOutputInfo>): Result<SelectedSoFar> {
            val tokensFoundResult: Pair<List<AssetOutputInfo>, List<AssetOutputInfo>> =
                selectForTokens(amounts.tokens, emptyList(), allUtxos)
                    .getOrElse { return Result.failure(it) }
            val (utxosForTokens, remainingUtxos) = tokensFoundResult

            val alphSelected = utxosForTokens.fold(U256.Zero) { acc, asset -> acc.addUnsafe(asset.output.amount) }
            val alphToSelect = amounts.alph.sub(alphSelected) ?: U256.Zero

            val alphFoundResult: Pair<List<AssetOutputInfo>, List<AssetOutputInfo>> =
                selectForAmount(alphToSelect, sortAlph(remainingUtxos)) { asset ->
                    asset.output.amount
                }.getOrElse { return Result.failure(it) }

            val (utxosForAlph, restOfUtxos) = alphFoundResult
            val foundUtxos = utxosForTokens + utxosForAlph
            val attoAlphAmountWithoutGas =
                foundUtxos.fold(U256.Zero) { acc, asset -> acc.addUnsafe(asset.output.amount) }
            return Result.success(SelectedSoFar(attoAlphAmountWithoutGas, foundUtxos, restOfUtxos))
        }

        private fun sortAlph(assets: List<AssetOutputInfo>): List<AssetOutputInfo> {
            val assetsWithoutTokens = assets.filter { it.output.tokens.isEmpty() }
            val assetsWithTokens = assets.filter { it.output.tokens.isNotEmpty() }
            return assetsWithoutTokens.sortedWith(assetOrder.byAlph) + assetsWithTokens.sortedWith(assetOrder.byAlph)
        }

        private fun selectForAmount(
            amount: U256,
            sortedUtxos: List<AssetOutputInfo>,
            getAmount: (AssetOutputInfo) -> U256,
        ): Result<Pair<List<AssetOutputInfo>, List<AssetOutputInfo>>> {
            if (amount == U256.Zero) return Result.success(Pair(emptyList(), sortedUtxos))

            val (sum, index) = sortedUtxos.foldIndexed(Pair(U256.Zero, -1)) { idx, acc, asset ->
                if (acc.first >= amount) {
                    acc
                } else {
                    Pair(acc.first.addUnsafe(getAmount(asset)), idx)
                }
            }

            return if (sum < amount) {
                val sumDec = sum.v.toBigDecimal()
                val amountDec = amount.v.toBigDecimal()
                Result.failure(
                    BlockchainSdkError.Alephium.NotEnoughBalance(
                        gotSum = sumDec,
                        expectedAmount = amountDec,
                        customMessage = "Not enough balance: got ${sumDec.toPlainString()}, expected ${
                        amountDec.toPlainString()
                        }",
                    ),
                )
            } else {
                Result.success(Pair(sortedUtxos.take(index + 1), sortedUtxos.drop(index + 1)))
            }
        }

        private fun selectForTokens(
            totalAmountPerToken: List<Pair<TokenId, U256>>,
            currentUtxos: List<AssetOutputInfo>,
            restOfUtxos: List<AssetOutputInfo>,
        ): Result<Pair<List<AssetOutputInfo>, List<AssetOutputInfo>>> {
            if (totalAmountPerToken.isEmpty()) return Result.success(Pair(currentUtxos, restOfUtxos))

            val (tokenId, amount) = totalAmountPerToken.first()
            val sortedUtxos = restOfUtxos.sortedWith(assetOrder.byToken(tokenId))
            val remainingTokenAmount = calculateRemainingTokensAmount(currentUtxos, tokenId, amount)

            val foundResult = selectForAmount(remainingTokenAmount, sortedUtxos) { asset ->
                asset.output.tokens.find { it.first.bytes().contentEquals(tokenId.value.bytes()) }
                    ?.second
                    ?: U256.Zero
            }

            return foundResult.map { (first, second) ->
                selectForTokens(
                    totalAmountPerToken.drop(1),
                    currentUtxos + first,
                    second,
                )
            }.getOrElse { Result.failure(it) }
        }

        private fun calculateRemainingTokensAmount(utxos: List<AssetOutputInfo>, tokenId: TokenId, amount: U256): U256 {
            val amountInUtxo = utxos.fold(U256.Zero) { acc, utxo ->
                utxo.output.tokens.fold(acc) { innerAcc, token ->
                    if (token.first.bytes()
                        .contentEquals(tokenId.value.bytes())
                    ) {
                        innerAcc.addUnsafe(token.second)
                    } else {
                        innerAcc
                    }
                }
            }
            return amount.sub(amountInUtxo) ?: U256.Zero
        }
    }
}