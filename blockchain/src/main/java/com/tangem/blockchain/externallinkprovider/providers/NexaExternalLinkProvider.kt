package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

class NexaExternalLinkProvider(isTestNet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestNet) {
            "https://explorer.nexa.org"
        } else {
            "https://testnet-explorer.nexa.org"
        }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "$explorerBaseUrl/address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("$explorerBaseUrl/tx/$transactionHash")
    }
}