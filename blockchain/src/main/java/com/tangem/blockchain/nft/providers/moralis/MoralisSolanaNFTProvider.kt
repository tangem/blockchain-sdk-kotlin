package com.tangem.blockchain.nft.providers.moralis

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaApi
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisSolanaNFTAssetResponse

internal class MoralisSolanaNFTProvider(
    private val blockchain: Blockchain,
    private val apiKey: String?,
) : NFTProvider {

    private val moralisSolanaApi = createRetrofitInstance(
        baseUrl = "https://solana-gateway.moralis.io/",
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put("X-API-Key", it) }
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
                collection = assetResponse.collection?.name,
            )
        }
        .mapValues { (collectionIdentifier, assetsResponse) ->
            val assets = assetsResponse.mapNotNull { assetResponse ->
                assetResponse.mint?.let { tokenAddress ->
                    val assetIdentifier = NFTAsset.Identifier.Solana(
                        tokenAddress = tokenAddress,
                        cnft = false,
                    )
                    assetResponse.toNFTAsset(
                        assetIdentifier = assetIdentifier,
                        collectionIdentifier = collectionIdentifier,
                        owner = walletAddress,
                    )
                }
            }

            val collectionResponse = assetsResponse.firstOrNull()?.collection
            if (collectionResponse?.name != null) {
                NFTCollection(
                    name = collectionResponse.name,
                    identifier = collectionIdentifier,
                    blockchainId = blockchain.id,
                    description = collectionResponse.description,
                    logoUrl = collectionResponse.imageOriginalUrl,
                    count = assets.size,
                    assets = assets,
                )
            } else {
                NFTCollection(
                    name = null,
                    identifier = collectionIdentifier,
                    blockchainId = blockchain.id,
                    description = null,
                    logoUrl = null,
                    count = assets.size,
                    assets = assets,
                )
            }
        }
        .values
        .toList()

    override suspend fun getAssets(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> {
        require(collectionIdentifier is NFTCollection.Identifier.Solana)
        return getCollections(walletAddress)
            .firstOrNull { it.identifier == collectionIdentifier }
            ?.assets
            .orEmpty()
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
        name = name,
        description = null,
        salePrice = null,
        rarity = null,
        media = toNFTAssetMedia(),
        traits = attributes?.mapNotNull {
            it.toNFTAssetTrait()
        }.orEmpty(),
    )

    private fun MoralisSolanaNFTAssetResponse.toNFTAssetMedia(): NFTAsset.Media? {
        val firstFile = properties?.files?.firstOrNull()
        return firstFile?.uri?.let {
            NFTAsset.Media(firstFile.type, it)
        }
    }

    private fun MoralisSolanaNFTAssetResponse.Attribute.toNFTAssetTrait(): NFTAsset.Trait? =
        if (traitType != null && value != null) {
            NFTAsset.Trait(traitType, value.toString())
        } else {
            null
        }
}