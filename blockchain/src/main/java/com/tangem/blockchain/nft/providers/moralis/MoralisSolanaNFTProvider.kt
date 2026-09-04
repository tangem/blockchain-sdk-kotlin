package com.tangem.blockchain.nft.providers.moralis

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.moralis.MoralisConstants
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.nft.extensions.ipfsToHttps
import com.tangem.blockchain.nft.extensions.nullIfEmpty
import com.tangem.blockchain.nft.extensions.removeUrlQuery
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaApi
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaNFTAssetResponse

internal class MoralisSolanaNFTProvider(
    private val blockchain: Blockchain,
    private val apiKey: String?,
) : NFTProvider {

    private val moralisSolanaApi = createRetrofitInstance(
        baseUrl = MoralisConstants.SOLANA_GATEWAY_API_URL,
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put(MoralisConstants.API_KEY_HEADER, it) }
                },
            ),
        ),
    ).create(MoralisSolanaApi::class.java)

    override suspend fun getCollections(walletAddress: String): List<NFTCollection> = moralisSolanaApi
        .getNFTAssets(
            address = walletAddress,
        )
        .groupBy { assetResponse ->
            NFTCollection.Identifier.Solana(
                collectionAddress = assetResponse.collection?.collectionAddress,
            )
        }
        .mapValues { (collectionIdentifier, assetsResponse) ->
            val assets = assetsResponse.mapNotNull { assetResponse ->
                val tokenAddress = assetResponse.mint

                if (tokenAddress == null) {
                    Logger.logNetwork(
                        "$LOG_TAG Asset missing required fields: mint=null, " +
                            "collection=${collectionIdentifier.collectionAddress}",
                    )
                    return@mapNotNull null
                }

                assetResponse.toNFTAsset(
                    assetIdentifier = NFTAsset.Identifier.Solana(
                        tokenAddress = tokenAddress,
                        tokenStandard = assetResponse.tokenStandard,
                    ),
                    collectionIdentifier = collectionIdentifier,
                    owner = walletAddress,
                )
            }

            val collectionResponse = assetsResponse.firstOrNull()?.collection
            NFTCollection(
                name = collectionResponse?.name,
                identifier = collectionIdentifier,
                blockchainId = blockchain.id,
                description = collectionResponse?.description,
                logoUrl = collectionResponse?.imageOriginalUrl?.ipfsToHttps()?.removeUrlQuery(),
                count = assets.size,
                assets = assets,
            )
        }
        .values
        .toList()

    override suspend fun getAssets(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> {
        throw UnsupportedOperationException(
            "Use getCollections() to get all assets grouped into collection",
        )
    }

    override suspend fun getAsset(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset? {
        throw UnsupportedOperationException(
            "Moralis Solana NFT API doesn't support this method, use getAssets() instead",
        )
    }

    override suspend fun getSalePrice(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice? {
        throw UnsupportedOperationException("Moralis Solana NFT API doesn't support this method")
    }

    private fun MoralisSolanaNFTAssetResponse.toNFTAsset(
        owner: String,
        assetIdentifier: NFTAsset.Identifier,
        collectionIdentifier: NFTCollection.Identifier.Solana,
    ): NFTAsset = NFTAsset(
        identifier = assetIdentifier,
        collectionIdentifier = collectionIdentifier,
        contractType = contract?.type.orEmpty(),
        blockchainId = blockchain.id,
        owner = owner,
        name = name?.nullIfEmpty(),
        amount = amount?.toBigInteger(),
        decimals = decimals ?: 0,
        description = null,
        salePrice = null,
        rarity = null,
        media = toNFTAssetMedia(collectionIdentifier),
        traits = attributes?.mapNotNull {
            it.toNFTAssetTrait()
        }.orEmpty(),
    )

    private fun MoralisSolanaNFTAssetResponse.toNFTAssetMedia(
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

    private fun MoralisSolanaNFTAssetResponse.Attribute.toNFTAssetTrait(): NFTAsset.Trait? =
        if (traitType != null && value != null) {
            NFTAsset.Trait(traitType, value.toString())
        } else {
            null
        }

    private companion object {
        const val LOG_TAG = "MoralisSolanaNFTProvider"
    }
}