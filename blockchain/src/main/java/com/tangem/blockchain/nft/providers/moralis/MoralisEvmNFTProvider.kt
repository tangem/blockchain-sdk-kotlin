package com.tangem.blockchain.nft.providers.moralis

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.nft.models.*
import com.tangem.blockchain.nft.providers.moralis.network.MoralisEvmApi
import com.tangem.blockchain.nft.providers.moralis.network.MoralisEvmNFTAssetResponse
import com.tangem.blockchain.nft.providers.moralis.network.MoralisEvmNFTCollectionResponse
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
                        contractName = it.name.orEmpty(),
                        contractAddress = tokenAddress,
                        blockchain = blockchain,
                        description = null,
                        logoUrl = it.collectionLogo,
                        count = it.count ?: 0,
                    )
                }
            }
    }

    override suspend fun getAssets(walletAddress: String, contractAddress: String): List<NFTAsset> {
        val accumulator = mutableListOf<MoralisEvmNFTAssetResponse>()
        var cursor: String? = null
        // implement pagination internally
        do {
            val response = moralisEvmApi.getNFTAssets(
                address = walletAddress,
                chain = blockchain.toQueryParam(),
                contractAddresses = listOf(contractAddress),
                limit = PAGINATION_LIMIT,
                cursor = cursor,
            )
            accumulator.addAll(response.result.orEmpty())
            cursor = response.cursor
        } while (cursor != null)

        return accumulator
            .map {
                val rarity = if (it.rarityLabel != null && it.rarityRank != null) {
                    NFTAssetRarity(it.rarityRank.toString(), it.rarityLabel)
                } else {
                    null
                }

                NFTAsset(
                    tokenId = it.tokenId.orEmpty(),
                    tokenAddress = it.tokenAddress,
                    contractAddress = contractAddress,
                    contractType = it.contractType.orEmpty(),
                    blockchain = blockchain,
                    owner = it.ownerOf,
                    name = it.normalizedMetadata?.name,
                    description = it.normalizedMetadata?.description,
                    salePrice = null,
                    rarity = rarity,
                    media = it.media?.let {
                        if (it.mimeType != null && it.mediaCollection?.high?.url != null) {
                            NFTAssetMedia(it.mimeType, it.mediaCollection.high.url)
                        } else {
                            null
                        }
                    },
                    traits = it.normalizedMetadata?.attributes?.mapNotNull {
                        if (it.traitType != null && it.value != null) {
                            NFTAssetTrait(it.traitType, it.value)
                        } else {
                            null
                        }
                    }.orEmpty(),
                )
            }
    }

    override suspend fun getSalePrice(
        walletAddress: String,
        contractAddress: String,
        tokenId: String,
    ): NFTAssetSalePrice? {
        val response = moralisEvmApi.getNFTPrice(
            contractAddress = contractAddress,
            tokenId = tokenId,
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
            NFTAssetSalePrice(
                value = price,
                symbol = tokenSymbol,
            )
        } else {
            null
        }
    }

    private fun Blockchain.toQueryParam(): String = this.getChainId()?.toHexString().orEmpty()

    private companion object {
        const val LAST_SALE_PRICE_DAYS = 365
        const val PAGINATION_LIMIT = 100
    }
}