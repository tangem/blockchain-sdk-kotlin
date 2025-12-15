package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class LineaExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://sepolia.lineascan.build/"
    } else {
        "https://lineascan.build/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}