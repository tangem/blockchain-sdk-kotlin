package com.tangem.blockchain.nft.providers.moralis.solana

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.nft.extensions.ipfsToHttps
import com.tangem.blockchain.nft.extensions.nullIfEmpty
import com.tangem.blockchain.nft.extensions.removeUrlQuery
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.models.SolanaNFTTokenStandard
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaNFTAssetResponse

/**
 * Assets are grouped by collection address alone: Moralis returns one collection under different names, and an
 * empty address the same as a missing one, so every addressless asset lands in a single "No collection".
 */
internal class MoralisSolanaLegacyNFTMapper(private val blockchain: Blockchain) {

    fun toCollections(assets: List<MoralisSolanaNFTAssetResponse>, owner: String): List<NFTCollection> = assets
        .groupBy { asset ->
            NFTCollection.Identifier.Solana(collectionAddress = asset.collection?.collectionAddress?.nullIfEmpty())
        }
        .map { (collectionIdentifier, assetsResponse) ->
            val collectionAssets = assetsResponse.mapNotNull { it.toAsset(collectionIdentifier, owner) }
            val collectionResponse = assetsResponse.firstOrNull()?.collection
            NFTCollection(
                name = collectionResponse?.name,
                identifier = collectionIdentifier,
                blockchainId = blockchain.id,
                description = collectionResponse?.description,
                logoUrl = collectionResponse?.imageOriginalUrl?.ipfsToHttps()?.removeUrlQuery(),
                count = collectionAssets.size,
                assets = collectionAssets,
            )
        }

    private fun MoralisSolanaNFTAssetResponse.toAsset(
        collectionIdentifier: NFTCollection.Identifier.Solana,
        owner: String,
    ): NFTAsset? {
        val tokenAddress = mint ?: run {
            Logger.logNetwork(
                "$LOG_TAG Asset missing required fields: mint=null, " +
                    "collection=${collectionIdentifier.collectionAddress}",
            )
            return null
        }

        return NFTAsset(
            identifier = NFTAsset.Identifier.Solana(
                tokenAddress = tokenAddress,
                tokenStandard = SolanaNFTTokenStandard.fromMetaplexIndex(tokenStandard),
            ),
            collectionIdentifier = collectionIdentifier,
            contractType = contract?.type.orEmpty(),
            blockchainId = blockchain.id,
            owner = owner,
            name = name?.nullIfEmpty(),
            amount = amount?.toBigInteger(),
            decimals = decimals ?: 0,
            // Moralis gives a Solana asset no description of its own. Its own collection's is taken rather than
            // the group's: assets grouped by address alone can carry different collection data.
            description = collection?.description?.nullIfEmpty(),
            salePrice = null,
            rarity = null,
            media = toMedia(collectionIdentifier),
            traits = attributes?.mapNotNull { it.toTrait() }.orEmpty(),
        )
    }

    private fun MoralisSolanaNFTAssetResponse.toMedia(
        collectionIdentifier: NFTCollection.Identifier.Solana,
    ): NFTAsset.Media? {
        val uri = properties?.files?.firstNotNullOfOrNull { it.uri?.nullIfEmpty() }

        // A legitimate token whose metadata Moralis hasn't indexed yet arrives with no media files. It is repaired
        // by a manual metadata resync, which needs the mint address.
        if (uri == null) {
            Logger.logNetwork(
                "$LOG_TAG Asset has no media: collection=${collectionIdentifier.collectionAddress}, mint=$mint, " +
                    "has_properties=${properties != null}, files=${properties?.files?.size ?: 0}",
            )
            return null
        }

        return NFTAsset.Media(
            animationUrl = null, // not implemented yet
            imageUrl = uri.ipfsToHttps().removeUrlQuery(),
        )
    }

    private fun MoralisSolanaNFTAssetResponse.Attribute.toTrait(): NFTAsset.Trait? =
        if (traitType != null && value != null) {
            NFTAsset.Trait(traitType, value.toString())
        } else {
            null
        }

    private companion object {
        const val LOG_TAG = "MoralisSolanaLegacyNFTMapper"
    }
}