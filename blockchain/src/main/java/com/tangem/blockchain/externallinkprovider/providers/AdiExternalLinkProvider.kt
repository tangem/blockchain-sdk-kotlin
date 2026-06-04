package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset

internal class AdiExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://explorer.ab.testnet.adifoundation.ai/"
    } else {
        "https://explorer.adifoundation.ai/"
    }

    override val testNetTopUpUrl: String? = if (isTestnet) {
        "https://sepolia.etherscan.io/token/0x2a98b46fe31ba8be05ef1ce3d36e1f80db04190d" +
            "?a=0xf5B0Ae14b62454782F79559aD28394213401d59B#readProxyContract"
    } else {
        null
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")
    }

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        return explorerBaseUrl + "nft/${assetIdentifier.tokenAddress}/${assetIdentifier.tokenId}"
    }
}