package com.tangem.blockchain.externallinkprovider

interface ExternalLinkProvider {

    val explorerBaseUrl: String
    val testNetTopUpUrl: String? get() = null

    fun explorerUrl(walletAddress: String, contractAddress: String?): String

    fun explorerTransactionUrl(transactionHash: String): String
}
