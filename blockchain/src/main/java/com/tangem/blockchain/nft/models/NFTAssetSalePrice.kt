package com.tangem.blockchain.nft.models

import java.math.BigDecimal

data class NFTAssetSalePrice(
    val value: BigDecimal,
    val symbol: String,
)