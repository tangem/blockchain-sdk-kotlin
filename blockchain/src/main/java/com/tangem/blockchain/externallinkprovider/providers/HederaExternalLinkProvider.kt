package com.tangem.blockchain.externallinkprovider.providers

import android.os.Build
import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class HederaExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://testnet.hederaexplorer.io/search-details/"
    } else {
        "https://hederaexplorer.io/search-details/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "account/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        // Hedera uses
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TxExploreState.Url(explorerBaseUrl + "transaction/$transactionHash")
        } else {
            TxExploreState.Unsupported
        }
    }
}