package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class KavaExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://explorer.testnet.kava.io/" else "https://explorer.kava.io/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://faucet.kava.io" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "tx/$transactionHash"
    }
}
