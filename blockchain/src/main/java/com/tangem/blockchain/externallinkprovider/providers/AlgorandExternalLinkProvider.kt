package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider

internal class AlgorandExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://explorer.bitquery.io/algorand_testnet/" else "https://explorer.bitquery.io/algorand/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://dispenser.testnet.aws.algodev.network" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return explorerBaseUrl + "address/$walletAddress/transactions"
    }

    override fun explorerTransactionUrl(transactionHash: String): String {
        return explorerBaseUrl + "tx/$transactionHash"
    }
}