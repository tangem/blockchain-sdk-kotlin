package com.tangem.blockchain.nft.providers.moralis.evm.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTPricesResponse(
    @Json(name = "last_sale") val lastSale: Sale?,
    @Json(name = "lowest_sale") val lowestSale: Sale?,
    @Json(name = "highest_sale") val highestSale: Sale?,
    @Json(name = "average_sale") val averageSale: AverageSale?,
    @Json(name = "total_trades") val totalTrades: Int?,
) {
    @JsonClass(generateAdapter = true)
    internal data class Sale(
        @Json(name = "transaction_hash") val transactionHash: String?,
        @Json(name = "block_timestamp") val blockTimestamp: String?,
        @Json(name = "buyer_address") val buyerAddress: String?,
        @Json(name = "seller_address") val sellerAddress: String?,
        @Json(name = "price") val price: String?,
        @Json(name = "price_formatted") val priceFormatted: String?,
        @Json(name = "usd_price_at_sale") val usdPriceAtSale: String?,
        @Json(name = "current_usd_value") val currentUsdValue: String?,
        @Json(name = "token_id") val tokenId: String?,
        @Json(name = "payment_token") val paymentToken: PaymentToken?,
    )

    @JsonClass(generateAdapter = true)
    internal data class PaymentToken(
        @Json(name = "token_name") val tokenName: String?,
        @Json(name = "token_symbol") val tokenSymbol: String?,
        @Json(name = "token_logo") val tokenLogo: String?,
        @Json(name = "token_decimals") val tokenDecimals: String?,
        @Json(name = "token_address") val tokenAddress: String?,
    )

    @JsonClass(generateAdapter = true)
    internal data class AverageSale(
        @Json(name = "price") val price: String?,
        @Json(name = "price_formatted") val priceFormatted: String?,
        @Json(name = "current_usd_value") val currentUsdValue: String?,
    )
}