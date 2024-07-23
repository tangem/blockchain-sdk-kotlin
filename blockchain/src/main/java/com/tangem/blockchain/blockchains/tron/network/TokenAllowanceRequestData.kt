package com.tangem.blockchain.blockchains.tron.network

data class TokenAllowanceRequestData(
    val ownerAddress: String,
    val contractAddress: String,
    val spenderAddress: String,
)