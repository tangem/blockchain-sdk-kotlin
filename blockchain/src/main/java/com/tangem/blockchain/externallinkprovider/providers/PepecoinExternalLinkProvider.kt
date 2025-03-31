package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class PepecoinExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {
    override val explorerBaseUrl: String = if (isTestnet) {
        "https://testnet.pepeblocks.com/"
    } else {
        "https://pepecoinexplorer.com/"
    }

    override val testNetTopUpUrl: String = "https://pepeblocks.com/faucet"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}