package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class FilecoinExternalLinkProvider : ExternalLinkProvider {

    override val explorerBaseUrl = "https://filfox.info/"

    override val testNetTopUpUrl = "https://faucet.calibnet.chainsafe-fil.io/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String) = TxExploreState.Unsupported
}