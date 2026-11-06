package com.tangem.blockchain.nft.providers.moralis.solana

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.nft.extensions.ipfsToHttps
import com.tangem.blockchain.nft.extensions.nullIfEmpty
import com.tangem.blockchain.nft.extensions.removeUrlQuery
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.models.SolanaNFTTokenStandard
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisWalletNFTsResponse
import java.math.BigInteger

/** A Core asset has no mint: the asset account address is its identifier, and its amount is always one. */
internal class MoralisSolanaCoreNFTMapper(private val blockchain: Blockchain) {

    fun toCollections(items: List<MoralisWalletNFTsResponse.Item>, owner: String): List<NFTCollection> = items
        .filter { it.tokenStandard == SolanaNFTTokenStandard.MPL_CORE }
        .mapNotNull { item ->
            val tokenAddress = item.tokenAddress?.nullIfEmpty() ?: run {
                Logger.logNetwork("$LOG_TAG Core asset without tokenAddress, collection=${item.collection?.address}")
                return@mapNotNull null
            }
            item to tokenAddress
        }
        .groupBy { (item, _) ->
            NFTCollection.Identifier.Solana(collectionAddress = item.collection?.address?.nullIfEmpty())
        }
        .map { (collectionIdentifier, entries) ->
            val assets = entries.map { (item, tokenAddress) -> item.toAsset(tokenAddress, collectionIdentifier, owner) }
            NFTCollection(
                name = entries.firstNotNullOfOrNull { (item, _) -> item.collection?.name?.nullIfEmpty() },
                identifier = collectionIdentifier,
                blockchainId = blockchain.id,
                description = null,
                logoUrl = assets.firstNotNullOfOrNull { it.media?.imageUrl },
                count = assets.size,
                assets = assets,
            )
        }

    private fun MoralisWalletNFTsResponse.Item.toAsset(
        tokenAddress: String,
        collectionIdentifier: NFTCollection.Identifier.Solana,
        owner: String,
    ) = NFTAsset(
        identifier = NFTAsset.Identifier.Solana(tokenAddress = tokenAddress, tokenStandard = tokenStandard),
        collectionIdentifier = collectionIdentifier,
        contractType = tokenStandard.orEmpty(),
        blockchainId = blockchain.id,
        owner = owner,
        name = name?.nullIfEmpty() ?: metadata?.name?.nullIfEmpty(),
        amount = BigInteger.ONE,
        decimals = 0,
        description = metadata?.description?.nullIfEmpty(),
        salePrice = null,
        rarity = null,
        media = toMedia(),
        traits = metadata?.attributes?.mapNotNull { it.toTrait() }.orEmpty(),
    )

    /**
     * The original media comes first: Moralis' resized copies of Core assets answer 403 while the originals load.
     * Moralis may point the main media at a video or audio file, which can't be a preview, so the metadata image
     * stands in for it.
     */
    private fun MoralisWalletNFTsResponse.Item.toMedia(): NFTAsset.Media? {
        val isImageMedia = media?.mimetype?.startsWith("image/") != false
        val originalImage = media?.originalMediaUrl?.nullIfEmpty()?.takeIf { isImageMedia }
        val mediaCollection = media?.mediaCollection
        val previewImage = if (isImageMedia) {
            mediaCollection?.high?.url?.nullIfEmpty()
                ?: mediaCollection?.medium?.url?.nullIfEmpty()
                ?: mediaCollection?.low?.url?.nullIfEmpty()
        } else {
            null
        }
        val url = originalImage ?: metadata?.image?.nullIfEmpty() ?: previewImage ?: return null
        return NFTAsset.Media(animationUrl = null, imageUrl = url.ipfsToHttps().removeUrlQuery())
    }

    private fun MoralisWalletNFTsResponse.Attribute.toTrait(): NFTAsset.Trait? {
        val name = traitType ?: return null
        val value = when (val raw = value) {
            null -> return null
            is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()
            else -> raw.toString()
        }
        return NFTAsset.Trait(name, value)
    }

    private companion object {
        const val LOG_TAG = "MoralisSolanaCoreNFTMapper"
    }
}