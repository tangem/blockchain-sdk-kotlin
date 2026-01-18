package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class TONExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) "https://testnet.tonviewer.com/" else "https://tonviewer.com/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://t.me/testgiver_ton_bot" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + walletAddress
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Unsupported
        // return explorerBaseUrl + "tx/$transactionHash" // disable for now
    }
}