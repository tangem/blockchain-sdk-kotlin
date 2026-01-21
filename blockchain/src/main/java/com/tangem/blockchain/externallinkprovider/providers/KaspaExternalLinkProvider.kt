package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class KaspaExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl = "https://explorer.kaspa.org/"

    private val tokenExplorerBaseUrl = "https://kaspa.stream/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return if (contractAddress != null) {
            tokenExplorerBaseUrl + "addresses/$walletAddress"
        } else {
            explorerBaseUrl + "addresses/$walletAddress"
        }
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "transactions/$transactionHash")
    }

    override fun getTokenExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(tokenExplorerBaseUrl + "transactions/$transactionHash")
    }
}