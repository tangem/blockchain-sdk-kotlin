package com.tangem.blockchain.nft

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTAssetSalePrice
import com.tangem.blockchain.nft.models.NFTCollection

internal class NFTEmptyProvider : NFTProvider {
    override suspend fun getCollections(walletAddress: String): List<NFTCollection> = emptyList()

    override suspend fun getAssets(walletAddress: String, contractAddress: String): List<NFTAsset> = emptyList()

    override suspend fun getSalePrice(
        walletAddress: String,
        contractAddress: String,
        tokenId: String,
    ): NFTAssetSalePrice? = null
}