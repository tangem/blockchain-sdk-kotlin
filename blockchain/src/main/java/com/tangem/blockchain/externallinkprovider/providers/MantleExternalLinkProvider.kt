package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset

internal class MantleExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl = "https://explorer.mantle.xyz/"

    override val explorerBaseUrl = if (isTestnet) {
        "https://explorer.testnet.mantle.xyz/"
    } else {
        "https://explorer.mantle.xyz/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}tx/$transactionHash")
    }

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        return explorerBaseUrl + "token/${assetIdentifier.tokenAddress}/instance/${assetIdentifier.tokenId}"
    }
}