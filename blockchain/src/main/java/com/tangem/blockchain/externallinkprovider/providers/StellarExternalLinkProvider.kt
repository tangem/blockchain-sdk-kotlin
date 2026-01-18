package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class StellarExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://testnet.stellarchain.io/" else "https://stellarchain.io/"

    override val testNetTopUpUrl: String? =
        if (isTestnet) "https://laboratory.stellar.org/#account-creator?network=test" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "accounts/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "transactions/$transactionHash")
    }
}