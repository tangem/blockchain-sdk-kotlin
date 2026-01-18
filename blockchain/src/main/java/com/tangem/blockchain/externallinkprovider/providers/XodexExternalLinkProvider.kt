package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class XodexExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://explorer.xo-dex.com/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}accounts/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}transactions/$transactionHash")
    }
}