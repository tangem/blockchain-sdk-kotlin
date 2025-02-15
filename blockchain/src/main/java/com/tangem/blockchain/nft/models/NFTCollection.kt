package com.tangem.blockchain.nft.models

import com.tangem.blockchain.common.Blockchain

data class NFTCollection(
    val identifier: Identifier,
    val blockchain: Blockchain,
    val name: String?,
    val description: String?,
    val logoUrl: String?,
    val count: Int,
    val assets: List<NFTAsset> = emptyList(),
) {
    sealed class Identifier {
        data class EVM(
            val tokenAddress: String,
        ) : Identifier()

        data class TON(
            val contractAddress: String?,
        ) : Identifier()
    }
}