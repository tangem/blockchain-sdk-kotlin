package com.tangem.blockchain.blockchains.cardano.network.adalite.converters

import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteUnspentOutputsResponse
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.common.extensions.hexToBytes
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteUnspentOutputsResponse.Utxo.Amount.Token as AdaliteUtxoToken

internal object AdaliteUnspentOutputConverter {

    fun convert(response: AdaliteUnspentOutputsResponse): List<CardanoUnspentOutput> {
        return response.successData
            .let { utxos ->
                if (DepsContainer.blockchainFeatureToggles.isCardanoTokenSupport) {
                    utxos
                } else {
                    // we need to ignore unspent outputs with tokens (until we start supporting tokens)
                    utxos.filter { it.amount.tokens.isEmpty() }
                }
            }
            .map { utxo ->
                CardanoUnspentOutput(
                    address = utxo.address,
                    amount = utxo.amount.value,
                    outputIndex = utxo.outputIndex.toLong(),
                    transactionHash = utxo.hash.hexToBytes(),
                    assets = utxo.amount.tokens.mapToAsset(),
                )
            }
    }

    private fun List<AdaliteUtxoToken>.mapToAsset(): List<CardanoUnspentOutput.Asset> {
        return map { token ->
            CardanoUnspentOutput.Asset(
                policyID = token.policyId,
                assetNameHex = token.assetName,
                amount = token.quantity.toLong(),
            )
        }
    }
}