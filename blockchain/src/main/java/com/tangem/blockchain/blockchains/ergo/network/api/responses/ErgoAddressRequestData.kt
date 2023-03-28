package com.tangem.blockchain.blockchains.ergo.network.api.responses

data class ErgoAddressRequestData(
    val address: String,
    val recentTransactions: List<String>
)