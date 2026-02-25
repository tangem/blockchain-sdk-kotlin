package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class BerachainExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl: String? = if (isTestnet) "https://bepolia.faucet.berachain.com/" else null

    override val explorerBaseUrl = if (isTestnet) {
        "https://testnet.berascan.com/"
    } else {
        "https://berascan.com/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}tx/$transactionHash")
    }
}