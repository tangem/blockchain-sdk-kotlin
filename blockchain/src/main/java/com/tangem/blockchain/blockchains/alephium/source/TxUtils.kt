package com.tangem.blockchain.blockchains.alephium.source

/**
 * https://github.com/alephium/alephium/blob/master/flow/src/main/scala/org/alephium/flow/core/TxUtils.scala
 */
internal object TxUtils {

    @Suppress("LongParameterList")
    fun transfer(
        fromLockupScript: LockupScript.Asset,
        fromUnlockScript: UnlockScript,
        outputInfos: TxOutputInfo,
        gasOpt: GasBox,
        gasPrice: GasPrice,
        utxos: List<AssetOutputInfo>,
        networkId: NetworkId,
    ): Result<UnsignedTransaction> {
        checkTotalAttoAlphAmount(outputInfos.attoAlphAmount)
        val totalAmountsE = UnsignedTransaction.calculateTotalAmountNeeded(outputInfos)

        val (totalAmount, totalAmountPerToken, txOutputLength) = totalAmountsE
        val selected: UtxoSelectionAlgo.Selected = selectUTXOs(
            totalAmount = totalAmount,
            totalAmountPerToken = totalAmountPerToken,
            gasOpt = gasOpt,
            gasPrice = gasPrice,
            utxos = utxos,
        ).getOrElse { return Result.failure(it) }
        val unsignedTx = UnsignedTransaction.buildTransferTx(
            fromLockupScript,
            fromUnlockScript,
            selected.assets.map { Pair(it.ref, it.output) },
            outputInfos,
            selected.gas,
            gasPrice,
            networkId,
        )
        return unsignedTx
    }

    private fun selectUTXOs(
        totalAmount: U256,
        totalAmountPerToken: List<Pair<TokenId, U256>>,
        gasOpt: GasBox,
        gasPrice: GasPrice,
        utxos: List<AssetOutputInfo>,
    ): Result<UtxoSelectionAlgo.Selected> {
        return UtxoSelectionAlgo.Build(UtxoSelectionAlgo.ProvidedGas(gasOpt, gasPrice, null))
            .select(
                UtxoSelectionAlgo.AssetAmounts(totalAmount, totalAmountPerToken),
                utxos,
            )
    }

    private fun checkTotalAttoAlphAmount(amount: U256): Result<U256> {
        return if (amount >= ALPH.MaxALPHValue) {
            Result.success(amount)
        } else {
            Result.failure(Exception("ALPH amount overflow"))
        }
    }
}