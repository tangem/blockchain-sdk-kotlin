package com.tangem.blockchain.nft.providers.moralis.solana

import com.tangem.blockchain.nft.models.NFTCollection

/**
 * Core assets come from a second endpoint; they are merged before collections are shown so a collection holding
 * both pNFTs and Core assets stays one collection. A Core asset without its own description shows the collection's,
 * and a collection without a logo takes the first Core asset's image.
 */
internal object MoralisSolanaCollectionsMerger {

    fun merge(legacy: List<NFTCollection>, core: List<NFTCollection>): List<NFTCollection> {
        if (core.isEmpty()) return legacy

        val coreByIdentifier = core.associateBy { it.identifier }
        val merged = legacy.map { collection ->
            val coreCollection = coreByIdentifier[collection.identifier] ?: return@map collection
            val coreAssets = coreCollection.assets.map { asset ->
                asset.copy(description = asset.description ?: collection.description)
            }
            val assets = collection.assets + coreAssets
            collection.copy(
                assets = assets,
                count = assets.size,
                logoUrl = collection.logoUrl ?: coreCollection.logoUrl,
            )
        }
        val legacyIdentifiers = legacy.map { it.identifier }.toSet()
        return merged + core.filterNot { it.identifier in legacyIdentifiers }
    }
}