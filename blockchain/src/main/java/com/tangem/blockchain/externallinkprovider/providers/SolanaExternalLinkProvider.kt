package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class SolanaExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    private val baseUrl = "https://explorer.solana.com/"
    private val testNetParam = "?cluster=testnet"

    override val explorerBaseUrl: String = if (isTestnet) "$baseUrl$testNetParam" else baseUrl

    override val testNetTopUpUrl: String? = if (isTestnet) "https://solfaucet.com/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return buildString {
            append(baseUrl)
            append("address/$walletAddress")
            if (isTestnet) append(testNetParam)
        }
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        val url = buildString {
            append(baseUrl)
            append("tx/$transactionHash")
            if (isTestnet) append(testNetParam)
        }
        return TxExploreState.Url(url)
    }
}