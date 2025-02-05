package com.tangem.blockchain.nft.providers.nftscan.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NFTScanTonNFTCollectionsResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val message: String?,
    @Json(name = "data") val data: List<NFTScanTonNFTCollectionResponse>,
)

@JsonClass(generateAdapter = true)
internal data class NFTScanTonNFTCollectionResponse(
    @Json(name = "contract_name") val contractName: String?,
    @Json(name = "contract_address") val contractAddress: String?,
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "owns_total") val ownsTotal: Int,
    @Json(name = "items_total") val itemsTotal: Int,
    @Json(name = "description") val description: String?,
    @Json(name = "assets") val assets: List<NFTScanTonNFTResponse>,
)

@JsonClass(generateAdapter = true)
internal data class NFTScanTonNFTResponse(
    @Json(name = "token_address") val tokenAddress: String,
    @Json(name = "contract_name") val contractName: String?,
    @Json(name = "contract_address") val contractAddress: String?,
    @Json(name = "token_id") val tokenId: String?,
    @Json(name = "block_number") val blockNumber: Int,
    @Json(name = "minter") val minter: String?,
    @Json(name = "owner") val owner: String,
    @Json(name = "mint_timestamp") val mintTimestamp: Long?,
    @Json(name = "mint_transaction_hash") val mintTransactionHash: String?,
    @Json(name = "mint_price") val mintPrice: Double?,
    @Json(name = "token_uri") val tokenUri: String?,
    @Json(name = "metadata_json") val metadataJson: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "content_type") val contentType: String?,
    @Json(name = "content_uri") val contentUri: String?,
    @Json(name = "image_uri") val imageUri: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "external_link") val externalLink: String?,
    @Json(name = "latest_trade_price") val latestTradePrice: Double?,
    @Json(name = "latest_trade_timestamp") val latestTradeTimestamp: Long?,
    @Json(name = "latest_trade_transaction_hash") val latestTradeTransactionHash: String?,
    @Json(name = "attributes") val attributes: String?,
)