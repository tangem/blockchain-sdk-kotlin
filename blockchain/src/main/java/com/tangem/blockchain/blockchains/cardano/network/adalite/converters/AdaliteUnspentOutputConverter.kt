package com.tangem.blockchain.blockchains.cardano.network.adalite.converters

import com.tangem.blockchain.blockchains.cardano.network.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteBalance
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteUnspentOutputsResponse
import com.tangem.common.extensions.hexToBytes

internal object AdaliteUnspentOutputConverter {

    fun convert(response: AdaliteUnspentOutputsResponse): List<CardanoUnspentOutput> {
        return response.successData
            // we need to ignore unspent outputs with tokens (until we start supporting tokens)
            .filter { it.amountData.tokens.isEmpty() }
            .map { utxo ->
                CardanoUnspentOutput(
                    address = utxo.address,
                    amount = utxo.amountData.amount,
                    outputIndex = utxo.outputIndex.toLong(),
                    transactionHash = utxo.hash.hexToBytes(),
                    assets = utxo.amountData.tokens.mapToAsset(),
                )
            }
    }

    private fun List<AdaliteBalance.Token>.mapToAsset(): List<CardanoUnspentOutput.Asset> {
        return map { token ->
            CardanoUnspentOutput.Asset(
                policyID = token.policyId,
                assetNameHex = token.assetName,
                amount = token.quantity,
            )
        }
    }
}