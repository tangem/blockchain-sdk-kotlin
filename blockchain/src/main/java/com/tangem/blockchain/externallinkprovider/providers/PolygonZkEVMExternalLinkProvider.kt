package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class PolygonZkEVMExternalLinkProvider(isTestNet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl = "https://faucet.quicknode.com/ethereum/sepolia/"

    override val explorerBaseUrl = if (isTestNet) {
        "https://cardona-zkevm.polygonscan.com/"
    } else {
        "https://zkevm.polygonscan.com/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url("${explorerBaseUrl}tx/$transactionHash")
    }
}