package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class FlareExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl = "https://faucet.flare.network/coston2/"

    override val explorerBaseUrl = if (isTestnet) {
        "https://coston2-explorer.flare.network/"
    } else {
        "https://flare-explorer.flare.network/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}tx/$transactionHash")
    }
}