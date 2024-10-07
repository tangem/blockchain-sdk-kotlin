package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class CoreExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://scan.test.btcs.network/"
    } else {
        "https://scan.coredao.org/"
    }

    override val testNetTopUpUrl: String = "https://scan.test.btcs.network/faucet"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}