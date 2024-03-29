package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class TelosExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) "https://testnet.teloscan.io/" else "https://teloscan.io/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://app.telos.net/testnet/developers" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")
    }
}
