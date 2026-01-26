package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class MonadExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl = "https://faucet.monad.xyz/"

    override val explorerBaseUrl = if (isTestnet) {
        "https://testnet.monadscan.com/"
    } else {
        "https://monadscan.com/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}tx/$transactionHash")
    }
}