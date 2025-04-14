package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class AptosExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = "https://explorer.aptoslabs.com/"

    override val testNetTopUpUrl: String = "https://www.aptosfaucet.com/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return createExplorerUrl(path = "account/$walletAddress")
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(createExplorerUrl(path = "txn/$transactionHash"))
    }

    private fun createExplorerUrl(path: String): String {
        return "$explorerBaseUrl$path?network=${if (isTestnet) "testnet" else "mainnet"}"
    }
}