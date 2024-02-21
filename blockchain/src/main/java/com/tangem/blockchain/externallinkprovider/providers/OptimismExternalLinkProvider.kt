package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class OptimismExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://goerli-optimism.etherscan.io/" else "https://optimistic.etherscan.io/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://optimismfaucet.xyz/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return if (contractAddress != null) {
            explorerBaseUrl + "token/$contractAddress?a=$walletAddress"
        } else {
            explorerBaseUrl + "address/$walletAddress"
        }
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")
    }
}