package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.common.Amount

data class TokenApproveRequestData(
    val ownerAddress: String,
    val contractAddress: String,
    val spenderAddress: String,
    val amount: Amount,
)