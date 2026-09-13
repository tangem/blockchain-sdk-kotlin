package com.tangem.blockchain.nft.providers.moralis

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
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
import com.tangem.blockchain.nft.providers.moralis.evm.network.*
import java.math.BigDecimal

internal class MoralisEvmNFTProvider(
    private val blockchain: Blockchain,
    private val apiKey: String?,
) : NFTProvider {

    private val moralisEvmApi = createRetrofitInstance(
        baseUrl = MoralisConstants.DEEP_INDEX_API_URL,
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put(MoralisConstants.API_KEY_HEADER, it) }
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
            .mapNotNull { collectionResponse ->
                val tokenAddress = collectionResponse.tokenAddress

                if (tokenAddress == null) {
                    Logger.logNetwork(
                        "$LOG_TAG Collection missing required fields: token_address=null, " +
                            "chain=${blockchain.toQueryParam()}, name=${collectionResponse.name}",
                    )
                    return@mapNotNull null
                }

                NFTCollection(
                    name = collectionResponse.name.orEmpty(),
                    identifier = NFTCollection.Identifier.EVM(tokenAddress),
                    blockchainId = blockchain.id,
                    description = null,
                    logoUrl = collectionResponse.collectionLogo,
                    count = collectionResponse.count ?: 0,
                )
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

        return accumulator.mapNotNull { assetResponse ->
            val tokenId = assetResponse.tokenId

            if (tokenId == null) {
                Logger.logNetwork(
                    "$LOG_TAG Asset missing required fields: token_id=null, " +
                        "chain=${blockchain.toQueryParam()}, token_address=${collectionIdentifier.tokenAddress}",
                )
                return@mapNotNull null
            }

            assetResponse.toNFTAsset(
                assetIdentifier = NFTAsset.Identifier.EVM(
                    tokenId = tokenId.toBigInteger(),
                    tokenAddress = collectionIdentifier.tokenAddress,
                    contractType = assetResponse.toContractType(),
                ),
                collectionIdentifier = collectionIdentifier,
            )
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
                    tokenId = assetIdentifier.tokenId.toString(),
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
            tokenId = assetIdentifier.tokenId.toString(),
            chain = blockchain.toQueryParam(),
            days = LAST_SALE_PRICE_DAYS,
        )

        val price = try {
            BigDecimal(response.lastSale?.priceFormatted.orEmpty())
        } catch (_: NumberFormatException) {
            null
        }

        return if (price != null) {
            NFTAsset.SalePrice(
                value = price,
                symbol = response.lastSale?.paymentToken?.tokenSymbol,
                decimals = response.lastSale?.paymentToken?.tokenDecimals?.toIntOrNull(),
            )
        } else {
            null
        }
    }

    private fun Blockchain.toQueryParam(): String =
        HEX_PREFIX + this.getChainId()?.let { Integer.toHexString(it) }.orEmpty()

    private fun MoralisEvmNFTAssetResponse.toNFTAsset(
        assetIdentifier: NFTAsset.Identifier,
        collectionIdentifier: NFTCollection.Identifier.EVM,
    ): NFTAsset = NFTAsset(
        identifier = assetIdentifier,
        collectionIdentifier = collectionIdentifier,
        contractType = contractType.orEmpty(),
        blockchainId = blockchain.id,
        owner = ownerOf,
        // Moralis leaves normalized_metadata empty for a token it hasn't indexed yet, while the top-level name
        // (taken from the contract) is still there — without the fallback such a token renders nameless.
        name = normalizedMetadata?.name?.nullIfEmpty() ?: name?.nullIfEmpty(),
        description = normalizedMetadata?.description?.nullIfEmpty(),
        amount = amount?.toBigInteger(),
        decimals = 0,
        salePrice = null,
        rarity = toNFTAssetRarity(),
        media = toNFTAssetMedia(collectionIdentifier),
        traits = normalizedMetadata?.attributes?.mapNotNull {
            it.toNFTAssetTrait()
        }.orEmpty(),
    )

    private fun MoralisEvmNFTAssetResponse.toNFTAssetMedia(
        collectionIdentifier: NFTCollection.Identifier.EVM,
    ): NFTAsset.Media? {
        val assetMedia = media
        val mediaCollection = assetMedia?.mediaCollection

        val url = mediaCollection?.high?.url?.nullIfEmpty()
            ?: mediaCollection?.medium?.url?.nullIfEmpty()
            ?: mediaCollection?.low?.url?.nullIfEmpty()
            ?: assetMedia?.originalMediaUrl?.nullIfEmpty()

        // Moralis serves a legitimate token with no media at all when it hasn't indexed its metadata yet. Such a
        // token is repaired by a manual metadata resync, which needs the chain, the contract and the token id — so
        // log the identity together with the fields that tell an unindexed token apart from a spam or media-less one.
        if (url == null) {
            Logger.logNetwork(
                "$LOG_TAG Asset has no media: chain=${blockchain.toQueryParam()}, " +
                    "token_address=${collectionIdentifier.tokenAddress}, token_id=$tokenId, " +
                    "media_status=${assetMedia?.status}, has_metadata=${metadata != null}, " +
                    "has_normalized_metadata=${normalizedMetadata != null}, token_uri=$tokenUri, " +
                    "last_metadata_sync=$lastMetadataSync, possible_spam=$possibleSpam",
            )
            return null
        }

        return NFTAsset.Media(
            animationUrl = null, // not implemented yet
            imageUrl = url.ipfsToHttps().removeUrlQuery(),
        )
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

    private fun MoralisEvmNFTAssetResponse.toContractType(): NFTAsset.Identifier.EVM.ContractType =
        NFTAsset.Identifier.EVM.ContractType.entries.firstOrNull {
            it.value == contractType.orEmpty()
        } ?: NFTAsset.Identifier.EVM.ContractType.Unknown

    private companion object {
        const val LOG_TAG = "MoralisEvmNFTProvider"
        const val LAST_SALE_PRICE_DAYS = 365
        const val PAGINATION_LIMIT = 100
    }
}