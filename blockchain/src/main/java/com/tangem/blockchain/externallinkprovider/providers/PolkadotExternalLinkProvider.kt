package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class PolkadotExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://westend.subscan.io/" else "https://assethub-polkadot.subscan.io/"

    override val testNetTopUpUrl: String? = if (isTestnet) {
        "https://matrix.to/#/!cJFtAIkwxuofiSYkPN:matrix.org?via=matrix.org&via=matrix.parity.io&via=web3.foundation"
    } else {
        null
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "account/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "extrinsic/$transactionHash")
    }
}