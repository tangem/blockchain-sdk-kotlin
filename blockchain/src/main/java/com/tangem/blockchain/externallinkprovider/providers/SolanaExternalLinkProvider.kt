package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class SolanaExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    private val baseUrl = "https://explorer.solana.com/"
    private val testNetParam = "?cluster=testnet"

    override val explorerBaseUrl: String = if (isTestnet) "$baseUrl$testNetParam" else baseUrl

    override val testNetTopUpUrl: String? = if (isTestnet) "https://solfaucet.com/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return buildString {
            append(baseUrl)
            append("address/$walletAddress")
            if (contractAddress != null) append("tokens")
            if (isTestnet) append(testNetParam)
        }
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return buildString {
            append(baseUrl)
            append("tx/$transactionHash")
            if (isTestnet) append(testNetParam)
        }
    }
}
