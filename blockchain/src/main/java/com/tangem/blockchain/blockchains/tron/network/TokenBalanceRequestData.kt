package com.tangem.blockchain.blockchains.tron.network

data class TokenBalanceRequestData(
    val address: String,
    val contractAddress: String,
)
