package com.tangem.blockchain.blockchains.algorand.models

internal data class AlgorandTransactionBuildParams(
    val genesisId: String,
    val genesisHash: String,
    val firstRound: Long,
    val lastRound: Long,
)