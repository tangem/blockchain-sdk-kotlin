package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class CosmosExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://explorer.theta-testnet.polypore.xyz/" else "https://www.mintscan.io/cosmos/"

    override val testNetTopUpUrl: String? =
        if (isTestnet) "https://discord.com/channels/669268347736686612/953697793476821092" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        val path = if (isTestnet) "accounts/$walletAddress" else "address/$walletAddress"
        return explorerBaseUrl + path
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        val path = if (isTestnet) "transactions/$transactionHash" else "tx/$transactionHash"
        return TxExploreState.Url(explorerBaseUrl + path)
    }
}