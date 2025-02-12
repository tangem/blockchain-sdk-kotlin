package com.tangem.blockchain.nft.models

import com.tangem.blockchain.common.Blockchain

data class NFTCollection(
    val contractName: String,
    val contractAddress: String,
    val blockchain: Blockchain,
    val description: String?,
    val logoUrl: String?,
    val count: Int,
    val assets: List<NFTAsset> = emptyList(),
)