package com.tangem.blockchain.nft

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection

interface NFTProvider {
    suspend fun getCollections(walletAddress: String): List<NFTCollection>

    suspend fun getAssets(walletAddress: String, collectionIdentifier: NFTCollection.Identifier): List<NFTAsset>

    suspend fun getAsset(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset?

    suspend fun getSalePrice(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice?
}