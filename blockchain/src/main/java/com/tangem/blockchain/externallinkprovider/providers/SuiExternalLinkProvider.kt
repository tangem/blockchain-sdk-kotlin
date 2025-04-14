package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class SuiExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://suiscan.xyz/testnet"
    } else {
        "https://suiscan.xyz/mainnet"
    }

    override val testNetTopUpUrl: String
        get() = "https://discord.com/channels/916379725201563759"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "$explorerBaseUrl/account/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "$explorerBaseUrl/tx/$transactionHash")
    }
}