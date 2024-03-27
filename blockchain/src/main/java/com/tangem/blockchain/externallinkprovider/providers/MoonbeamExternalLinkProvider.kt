package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class MoonbeamExternalLinkProvider(isTestNet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl = "https://faucet.moonbeam.network/"

    override val explorerBaseUrl = if (isTestNet) {
        "https://moonbase.moonscan.io/"
    } else {
        "https://moonscan.io/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}