package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset

internal class BlastLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl = if (isTestnet) "https://sepolia.blastexplorer.io/" else "https://blastscan.io/"

    override val testNetTopUpUrl: String?
        get() = super.testNetTopUpUrl

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}tx/$transactionHash")
    }

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        return explorerBaseUrl + "nft/${assetIdentifier.tokenAddress}/${assetIdentifier.tokenId}"
    }
}