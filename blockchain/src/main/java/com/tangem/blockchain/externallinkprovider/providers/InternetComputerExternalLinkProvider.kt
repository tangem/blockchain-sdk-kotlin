package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class InternetComputerExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://dashboard.internetcomputer.org/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "$explorerBaseUrl/account/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url("$explorerBaseUrl/transaction/$transactionHash")
    }
}