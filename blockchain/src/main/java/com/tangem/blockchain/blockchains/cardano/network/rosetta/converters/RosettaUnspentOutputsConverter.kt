package com.tangem.blockchain.blockchains.cardano.network.rosetta.converters

import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.common.extensions.hexToBytes
import com.tangem.blockchain.blockchains.cardano.network.rosetta.response.RosettaCoinsResponse.Coin as RosettaCoin

internal object RosettaUnspentOutputsConverter {

    fun convert(coinsMap: Map<String, List<RosettaCoin>>): List<CardanoUnspentOutput> {
        return coinsMap.flatMap { entry ->
            entry.value.map { coin ->
                val (transactionHash, outputIndex) = coin.coinIdentifier.getTransactionHashAndOutputIndex()

                CardanoUnspentOutput(
                    address = entry.key,
                    amount = coin.amount.value.toLong(),
                    outputIndex = outputIndex,
                    transactionHash = transactionHash,
                    assets = coin.metadata?.mapToAsset() ?: emptyList(),
                )
            }
        }
    }

    /*
     * Identifier format – transactionHash:outputIndex
     * Example: 482d88eb2d3b40b8a4e6bb8545cef842a5703e8f9eab9e3caca5c2edd1f31a7f:0
     */
    private fun RosettaCoin.CoinIdentifier.getTransactionHashAndOutputIndex(): Pair<ByteArray, Long> {
        val parts = identifier.split(":")
        return parts[0].hexToBytes() to parts[1].toLong()
    }

    private fun Map<String, RosettaCoin.MetadataValue>.mapToAsset(): List<CardanoUnspentOutput.Asset> {
        return values.flatMap {
            it.tokens.mapNotNull { amount ->
                CardanoUnspentOutput.Asset(
                    policyID = amount.currency.metadata?.policyId ?: return@mapNotNull null,
                    assetNameHex = amount.currency.symbol ?: return@mapNotNull null,
                    amount = amount.value.toLong(),
                )
            }
        }
    }
}