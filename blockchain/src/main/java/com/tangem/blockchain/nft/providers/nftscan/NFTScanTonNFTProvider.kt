package com.tangem.blockchain.nft.providers.nftscan

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.nft.models.*
import com.tangem.blockchain.nft.providers.nftscan.network.NFTScanTonApi
import com.tangem.blockchain.nft.providers.nftscan.network.NFTScanTonAssetResponse
import com.tangem.blockchain.nft.providers.nftscan.network.NFTScanTonNFTAttributeResponse
import java.math.BigDecimal

internal class NFTScanTonNFTProvider(
    private val apiKey: String?,
) : NFTProvider {

    private val nftScanTonApi = createRetrofitInstance(
        baseUrl = "https://tonapi.nftscan.com/",
        headerInterceptors = listOf(
            AddHeaderInterceptor(
                headers = buildMap {
                    apiKey?.let { put("X-API-Key", it) }
                },
            ),
        ),
    ).create(NFTScanTonApi::class.java)

    override suspend fun getCollections(walletAddress: String): List<NFTCollection> {
        val response = nftScanTonApi.getNFTCollections(
            accountAddress = walletAddress,
        )
        return response.data.orEmpty().map {
            val collectionIdentifier = NFTCollection.Identifier.TON(it.contractAddress)
            NFTCollection(
                name = it.contractName,
                identifier = collectionIdentifier,
                blockchain = Blockchain.TON,
                description = it.description,
                logoUrl = it.logoUrl,
                count = it.ownsTotal ?: 0,
                assets = it.assets?.map { it.toNFTAsset(collectionIdentifier) }.orEmpty(),
            )
        }
    }

    override suspend fun getAssets(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
    ): List<NFTAsset> {
        require(collectionIdentifier is NFTCollection.Identifier.TON)

        if (collectionIdentifier.contractAddress == null) {
            // we can't get assets without contract address using separate request
            return getCollections(walletAddress)
                .filter { it.identifier == collectionIdentifier }
                .flatMap { it.assets }
        }

        val accumulator = mutableListOf<NFTScanTonAssetResponse>()
        var cursor: String? = null
        // implement pagination internally
        do {
            val response = nftScanTonApi.getNFTAssets(
                accountAddress = walletAddress,
                contractAddress = collectionIdentifier.contractAddress,
                cursor = cursor,
                limit = PAGINATION_LIMIT,
            )
            accumulator.addAll(response.data?.content.orEmpty())
            cursor = response.data?.next
        } while (cursor != null)
        return accumulator.map {
            it.toNFTAsset(collectionIdentifier)
        }
    }

    override suspend fun getSalePrice(
        walletAddress: String,
        collectionIdentifier: NFTCollection.Identifier,
        assetIdentifier: NFTAsset.Identifier,
    ): NFTAsset.SalePrice? {
        require(collectionIdentifier is NFTCollection.Identifier.TON)
        require(assetIdentifier is NFTAsset.Identifier.TON)
        val response = nftScanTonApi.getNFTAsset(
            tokenAddress = assetIdentifier.tokenAddress,
        )
        return response.data?.toNFTAssetSalePrice()
    }

    private fun NFTScanTonAssetResponse.toNFTAsset(collectionIdentifier: NFTCollection.Identifier): NFTAsset = NFTAsset(
        identifier = NFTAsset.Identifier.TON(tokenAddress),
        collectionIdentifier = collectionIdentifier,
        contractType = TEP62_STANDARD,
        blockchain = Blockchain.TON,
        owner = owner,
        name = name.orEmpty(),
        description = description,
        salePrice = toNFTAssetSalePrice(),
        rarity = null,
        media = toNFTAssetMedia(),
        traits = attributes?.mapNotNull {
            it.toNFTAssetTrait()
        }.orEmpty(),
    )

    private fun NFTScanTonAssetResponse.toNFTAssetMedia(): NFTAsset.Media? =
        if (contentType != null && imageUri != null) {
            NFTAsset.Media(contentType, imageUri)
        } else {
            null
        }

    private fun NFTScanTonAssetResponse.toNFTAssetSalePrice(): NFTAsset.SalePrice? = if (latestTradePrice != null) {
        NFTAsset.SalePrice(
            value = BigDecimal.valueOf(latestTradePrice),
            symbol = Blockchain.TON.currency,
        )
    } else {
        null
    }

    private fun NFTScanTonNFTAttributeResponse.toNFTAssetTrait(): NFTAsset.Trait? =
        if (attributeName != null && attributeValue != null) {
            NFTAsset.Trait(attributeName, attributeValue)
        } else {
            null
        }

    private companion object {
        const val TEP62_STANDARD = "TEP-62"
        const val PAGINATION_LIMIT = 100
    }
}