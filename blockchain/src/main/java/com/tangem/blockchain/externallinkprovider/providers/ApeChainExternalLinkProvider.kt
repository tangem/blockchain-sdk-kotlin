package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class ApeChainExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {
    override val explorerBaseUrl: String = if (isTestnet) {
        "https://curtis.explorer.caldera.xyz/"
    } else {
        "https://apechain.calderaexplorer.xyz/"
    }

    override val testNetTopUpUrl: String = "https://curtis.hub.caldera.xyz/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}