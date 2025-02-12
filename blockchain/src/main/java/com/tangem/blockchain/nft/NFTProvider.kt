package com.tangem.blockchain.nft

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTAssetSalePrice
import com.tangem.blockchain.nft.models.NFTCollection

interface NFTProvider {
    suspend fun getCollections(walletAddress: String, blockchain: Blockchain): List<NFTCollection>

    suspend fun getAssets(walletAddress: String, blockchain: Blockchain, contractAddress: String): List<NFTAsset>

    suspend fun getSalePrice(
        walletAddress: String,
        blockchain: Blockchain,
        contractAddress: String,
        tokenId: String,
    ): NFTAssetSalePrice
}