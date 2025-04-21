package com.tangem.blockchain.nft.providers.moralis

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.nft.models.*
import com.tangem.blockchain.nft.providers.moralis.evm.network.*
import com.tangem.blockchain.nft.providers.moralis.evm.network.MoralisEvmApi
import com.tangem.blockchain.nft.providers.moralis.evm.network.MoralisEvmNFTAssetResponse
import com.tangem.blockchain.nft.providers.moralis.evm.network.MoralisEvmNFTCollectionResponse
import okhttp3.internal.toHexString
import java.math.BigDecimal

internal class MoralisEvmNFTProvider(
    private val blockchain: Blockchain,
    private val apiKey: String?,
) : NFTProvider {

    private val moralisEvmApi = createRetrofitInstance(
        baseUrl = "https://deep-index.moralis.io/",
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put("X-API-Key", it) }
                },
            ),
        ),
    ).create(MoralisEvmApi::class.java)

    override suspend fun getCollections(walletAddress: String): List<NFTCollection> {
        val accumulator = mutableListOf<MoralisEvmNFTCollectionResponse>()
        var cursor: String? = null
        // implement pagination internally
        do {
            val response = moralisEvmApi.getNFTCollections(
                address = walletAddress,
                chain = blockchain.toQueryParam(),
                limit = PAGINATION_LIMIT,
                cursor = cursor,
            )
            accumulator.addAll(response.result.orEmpty())
            cursor = response.cursor
        } while (cursor != null)

        return accumulator
            .mapNotNull {
                it.tokenAddress?.let { tokenAddress ->
                    NFTCollection(
                        name = it.name.orEmpty(),
                        identifier = NFTCollection.Identifier.EVM(tokenAddress),
                        blockchainId = blockchain.id,
                        description = null,
                        logoUrl = it.collectionLogo,
                        count = it.count ?: 0,
                    )
                }
            }
    }

    override suspend fun getAssets(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> {
        require(collectionIdentifier is NFTCollection.Identifier.EVM)
        val accumulator = mutableListOf<MoralisEvmNFTAssetResponse>()
        var cursor: String? = null
        // implement pagination internally
        do {
            val response = moralisEvmApi.getNFTAssets(
                address = walletAddress,
                chain = blockchain.toQueryParam(),
                tokenAddresses = listOf(collectionIdentifier.tokenAddress),
                limit = PAGINATION_LIMIT,
                cursor = cursor,
            )
            accumulator.addAll(response.result.orEmpty())
            cursor = response.cursor
        } while (cursor != null)

        return accumulator.mapNotNull {
            it.tokenId?.let { tokenId ->
                it.toNFTAsset(
                    assetIdentifier = NFTAsset.Identifier.EVM(
                        tokenId = tokenId,
                        tokenAddress = collectionIdentifier.tokenAddress,
                    ),
                    collectionIdentifier = collectionIdentifier,
                )
            }
        }
    }

    override suspend fun getAsset(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset? {
        require(collectionIdentifier is NFTCollection.Identifier.EVM)
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        val request = MoralisEvmNFTGetAssetsRequest(
            tokens = listOf(
                MoralisEvmNFTGetAssetsTokenRequest(
                    tokenAddress = assetIdentifier.tokenAddress,
                    tokenId = assetIdentifier.tokenId,
                ),
            ),
        )

        return moralisEvmApi
            .getNFTAssets(request)
            .firstOrNull()
            ?.toNFTAsset(
                assetIdentifier = assetIdentifier,
                collectionIdentifier = collectionIdentifier,
            )
    }

    override suspend fun getSalePrice(
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice? {
        require(collectionIdentifier is NFTCollection.Identifier.EVM)
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        val response = moralisEvmApi.getNFTPrice(
            tokenAddress = collectionIdentifier.tokenAddress,
            tokenId = assetIdentifier.tokenId,
            chain = blockchain.toQueryParam(),
            days = LAST_SALE_PRICE_DAYS,
        )

        val price = try {
            BigDecimal(response.lastSale?.priceFormatted.orEmpty())
        } catch (e: NumberFormatException) {
            null
        }

        val tokenSymbol = response.lastSale?.paymentToken?.tokenSymbol

        return if (price != null && tokenSymbol != null) {
            NFTAsset.SalePrice(
                value = price,
                symbol = tokenSymbol,
            )
        } else {
            null
        }
    }

    private fun Blockchain.toQueryParam(): String = HEX_PREFIX + this.getChainId()?.toHexString().orEmpty()

    private fun MoralisEvmNFTAssetResponse.toNFTAsset(
        assetIdentifier: NFTAsset.Identifier,
        collectionIdentifier: NFTCollection.Identifier.EVM,
    ): NFTAsset = NFTAsset(
        identifier = assetIdentifier,
        collectionIdentifier = collectionIdentifier,
        contractType = contractType.orEmpty(),
        blockchainId = blockchain.id,
        owner = ownerOf,
        name = normalizedMetadata?.name,
        description = normalizedMetadata?.description,
        salePrice = null,
        rarity = toNFTAssetRarity(),
        media = media?.toNFTAssetMedia(),
        traits = normalizedMetadata?.attributes?.mapNotNull {
            it.toNFTAssetTrait()
        }.orEmpty(),
    )

    private fun MoralisEvmNFTAssetResponse.Media.toNFTAssetMedia(): NFTAsset.Media? = when {
        mediaCollection?.high?.url != null -> NFTAsset.Media(mimeType, mediaCollection.high.url)
        mediaCollection?.medium?.url != null -> NFTAsset.Media(mimeType, mediaCollection.medium.url)
        originalMediaUrl != null -> NFTAsset.Media(mimeType, originalMediaUrl)
        else -> null
    }

    private fun MoralisEvmNFTAssetResponse.Attribute.toNFTAssetTrait(): NFTAsset.Trait? =
        if (traitType != null && value != null) {
            NFTAsset.Trait(traitType, value.toString())
        } else {
            null
        }

    private fun MoralisEvmNFTAssetResponse.toNFTAssetRarity(): NFTAsset.Rarity? =
        if (rarityLabel != null && rarityRank != null) {
            NFTAsset.Rarity(rarityRank.toString(), rarityLabel)
        } else {
            null
        }

    private companion object {
        const val LAST_SALE_PRICE_DAYS = 365
        const val PAGINATION_LIMIT = 100
    }
}