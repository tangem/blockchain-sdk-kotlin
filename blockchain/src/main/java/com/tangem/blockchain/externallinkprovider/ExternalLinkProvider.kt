package com.tangem.blockchain.externallinkprovider

import com.tangem.blockchain.nft.models.NFTAsset

interface ExternalLinkProvider {

    val explorerBaseUrl: String
    val testNetTopUpUrl: String? get() = null

    fun explorerUrl(walletAddress: String, contractAddress: String?): String

    fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String? = null

    fun getExplorerTxUrl(transactionHash: String): TxExploreState
}