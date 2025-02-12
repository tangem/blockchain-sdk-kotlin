package com.tangem.blockchain.nft.models

import com.tangem.blockchain.common.Blockchain

data class NFTAsset(
    val tokenId: String,
    val tokenAddress: String?,
    val contractAddress: String,
    val contractType: String,
    val blockchain: Blockchain,
    val owner: String?,
    val name: String?,
    val description: String?,
    val salePrice: NFTAssetSalePrice?,
    val rarity: NFTAssetRarity?,
    val media: NFTAssetMedia?,
    val traits: List<NFTAssetTrait>,
)