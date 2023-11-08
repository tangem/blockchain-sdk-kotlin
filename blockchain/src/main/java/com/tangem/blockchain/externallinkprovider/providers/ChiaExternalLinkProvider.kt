package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class ChiaExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) "https://testnet10.spacescan.io/" else "https://xchscan.com/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://xchdev.com/#!faucet.md" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "txns/$transactionHash"
    }
}
