package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class KoinosExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://harbinger.koinosblocks.com/"
    } else {
        "https://koinosblocks.com/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return if (isTestnet) {
            "${explorerBaseUrl}address/$walletAddress"
        } else {
            "${explorerBaseUrl}address/$walletAddress"
        }
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url(
            if (isTestnet) {
                "${explorerBaseUrl}tx/$transactionHash"
            } else {
                "${explorerBaseUrl}tx/$transactionHash"
            },
        )
    }
}