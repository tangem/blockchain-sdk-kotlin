package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class AreonExternalLinkProvider : ExternalLinkProvider {

    override val testNetTopUpUrl: String = "https://faucet.areon.network/"

    override val explorerBaseUrl: String = "https://areonscan.com/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}accounts/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}transactions/$transactionHash")
    }
}