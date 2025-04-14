package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class AlephZeroExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://alephzero.subscan.io/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "account/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "extrinsic/$transactionHash")
    }
}