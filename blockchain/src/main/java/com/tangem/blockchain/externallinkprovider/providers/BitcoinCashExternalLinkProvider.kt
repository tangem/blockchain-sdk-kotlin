package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class BitcoinCashExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://blockexplorer.one/bitcoin-cash/testnet/" else "https://www.blockchair.com/bitcoin-cash/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://coinfaucet.eu/en/bch-testnet/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return if (isTestnet) {
            explorerBaseUrl + "tx/$transactionHash"
        } else {
            explorerBaseUrl + "transaction/$transactionHash"
        }
    }
}
