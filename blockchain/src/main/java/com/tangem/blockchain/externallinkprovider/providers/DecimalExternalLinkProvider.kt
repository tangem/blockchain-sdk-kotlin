package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class DecimalExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://testnet.explorer.decimalchain.com/"
    } else {
        "https://explorer.decimalchain.com/"
    }

    override val testNetTopUpUrl: String = "https://testnet.console.decimalchain.com/wallet/"

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        val address = DecimalAddressService.convertErcAddressToDscAddress(walletAddress)

        return explorerBaseUrl + "address/$address"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "transactions/$transactionHash"
    }
}
