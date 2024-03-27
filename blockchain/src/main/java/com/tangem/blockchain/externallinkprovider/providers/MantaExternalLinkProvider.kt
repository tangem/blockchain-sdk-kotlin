package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class MantaExternalLinkProvider(isTestNet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl = "https://pacific-info.testnet.manta.network/"

    override val explorerBaseUrl: String =
        if (isTestNet) {
            "https://pacific-explorer.testnet.manta.network/"
        } else {
            "https://pacific-explorer.manta.network/"
        }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}