package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class PolkadotExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://westend.subscan.io/" else "https://polkadot.subscan.io/"

    override val testNetTopUpUrl: String? = if (isTestnet) {
        "https://matrix.to/#/!cJFtAIkwxuofiSYkPN:matrix.org?via=matrix.org&via=matrix.parity.io&via=web3.foundation"
    } else {
        null
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "account/$walletAddress"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "extrinsic/$transactionHash"
    }
}
