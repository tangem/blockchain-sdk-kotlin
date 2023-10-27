package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class BitcoinExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://www.blockchair.com/bitcoin/" else "https://www.blockchair.com/bitcoin/testnet/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://coinfaucet.eu/en/btc-testnet/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "transaction/$transactionHash"
    }
}