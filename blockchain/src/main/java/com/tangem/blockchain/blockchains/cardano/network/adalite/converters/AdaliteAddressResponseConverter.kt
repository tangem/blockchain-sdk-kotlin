package com.tangem.blockchain.blockchains.cardano.network.adalite.converters

import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteUnspentOutputsResponse
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse.AdaliteAddressResponse.SuccessData.Transaction as CardanoTransaction

internal object AdaliteAddressResponseConverter {

    fun convert(
        addressDataResponse: List<AdaliteAddressResponse>,
        unspentOutputsResponse: AdaliteUnspentOutputsResponse,
        tokens: Set<Token>,
    ): CardanoAddressResponse {
        val unspentOutputs = AdaliteUnspentOutputConverter.convert(unspentOutputsResponse)

        return CardanoAddressResponse(
            balance = unspentOutputs.sumOf(CardanoUnspentOutput::amount),
            tokenBalances = createTokenBalances(unspentOutputs, tokens),
            unspentOutputs = unspentOutputs,
            recentTransactionsHashes = addressDataResponse.getTransactionHashes(),
        )
    }

    private fun createTokenBalances(unspentOutputs: List<CardanoUnspentOutput>, tokens: Set<Token>): Map<Token, Long> {
        return tokens.associateWith { token ->
            val balance = unspentOutputs.flatMap(CardanoUnspentOutput::assets)
                .filter { token.contractAddress.startsWith(prefix = it.policyID) }
                .sumOf(CardanoUnspentOutput.Asset::amount)

            balance
        }
    }

    private fun List<AdaliteAddressResponse>.getTransactionHashes(): List<String> {
        return flatMap { it.successData.transactions!!.mapNotNull(CardanoTransaction::hash) }
    }
}