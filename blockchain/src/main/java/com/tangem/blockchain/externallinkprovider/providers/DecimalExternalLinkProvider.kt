package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState

internal class DecimalExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://testnet.explorer.decimalchain.com/"
    } else {
        "https://explorer.decimalchain.com/"
    }

    override val testNetTopUpUrl: String = "https://testnet.console.decimalchain.com/wallet/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        val address = DecimalAddressService.convertDscAddressToDelAddress(walletAddress)

        return explorerBaseUrl + "address/$address"
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "transactions/$transactionHash")
    }
}