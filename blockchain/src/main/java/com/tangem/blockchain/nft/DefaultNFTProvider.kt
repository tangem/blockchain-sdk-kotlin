package com.tangem.blockchain.nft

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection

internal object DefaultNFTProvider : NFTProvider {
    override suspend fun getCollections(walletAddress: String): List<NFTCollection> = emptyList()

    override suspend fun getAssets(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> = emptyList()

    override suspend fun getAsset(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset? = null

    override suspend fun getSalePrice(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice? = null
}