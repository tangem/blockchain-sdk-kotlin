package com.tangem.blockchain.blockchains.cardano.network.common.models

import com.tangem.blockchain.common.Token

internal data class CardanoAddressResponse(
    val balance: Long,
    val tokenBalances: Map<Token, Long>,
    val unspentOutputs: List<CardanoUnspentOutput>,
    val recentTransactionsHashes: List<String>,
)