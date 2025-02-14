package com.tangem.blockchain.nft

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTAssetSalePrice
import com.tangem.blockchain.nft.models.NFTCollection

interface NFTProvider {
    suspend fun getCollections(walletAddress: String): List<NFTCollection>

    suspend fun getAssets(walletAddress: String, contractAddress: String): List<NFTAsset>

    suspend fun getSalePrice(walletAddress: String, contractAddress: String, tokenId: String): NFTAssetSalePrice?
}