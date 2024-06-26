package com.tangem.blockchain.blockchains.cardano.network.common.converters

import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.utils.isCardanoAsset
import com.tangem.blockchain.common.Token

internal object CardanoAddressResponseConverter {

    fun convert(
        unspentOutputs: List<CardanoUnspentOutput>,
        tokens: Set<Token>,
        recentTransactionsHashes: List<String>,
    ): CardanoAddressResponse {
        return CardanoAddressResponse(
            balance = unspentOutputs.sumOf(CardanoUnspentOutput::amount),
            tokenBalances = createTokenBalances(unspentOutputs, tokens),
            unspentOutputs = unspentOutputs,
            recentTransactionsHashes = recentTransactionsHashes,
        )
    }

    private fun createTokenBalances(unspentOutputs: List<CardanoUnspentOutput>, tokens: Set<Token>): Map<Token, Long> {
        return tokens.associateWith { token ->
            unspentOutputs.flatMap(CardanoUnspentOutput::assets)
                .filter { asset ->
                    token.contractAddress.isCardanoAsset(policyId = asset.policyID, assetName = asset.assetName)
                }
                .sumOf(CardanoUnspentOutput.Asset::amount)
        }
    }
}