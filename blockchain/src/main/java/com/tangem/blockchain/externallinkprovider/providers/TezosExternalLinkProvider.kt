package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class TezosExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://tzkt.io/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "$explorerBaseUrl$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")
    }
}