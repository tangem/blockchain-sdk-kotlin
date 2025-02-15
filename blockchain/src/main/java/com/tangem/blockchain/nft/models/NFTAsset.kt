package com.tangem.blockchain.nft.models

import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

data class NFTAsset(
    val identifier: Identifier,
    val collectionIdentifier: NFTCollection.Identifier,
    val blockchain: Blockchain,
    val contractType: String,
    val owner: String?,
    val name: String?,
    val description: String?,
    val salePrice: SalePrice?,
    val rarity: Rarity?,
    val media: Media?,
    val traits: List<Trait>,
) {
    sealed class Identifier {
        data class EVM(val tokenId: String) : Identifier()
        data class TON(val tokenAddress: String) : Identifier()
    }

    data class Media(
        val mimetype: String,
        val url: String,
    )

    data class Rarity(
        val rank: String,
        val label: String,
    )

    data class Trait(
        val name: String,
        val value: String,
    )

    data class SalePrice(
        val value: BigDecimal,
        val symbol: String,
    )
}