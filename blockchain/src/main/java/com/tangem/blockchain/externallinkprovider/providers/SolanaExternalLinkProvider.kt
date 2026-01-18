package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset

internal class SolanaExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    private val baseUrl = "https://solscan.io/"
    private val testNetParam = "?cluster=testnet"

    override val explorerBaseUrl: String = if (isTestnet) "$baseUrl$testNetParam" else baseUrl

    override val testNetTopUpUrl: String? = if (isTestnet) "https://solfaucet.com/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return buildString {
            append(baseUrl)
            append("account/$walletAddress")
            if (isTestnet) append(testNetParam)
        }
    }

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.Solana)
        return buildString {
            append(baseUrl)
            append("token/${assetIdentifier.tokenAddress}")
            if (isTestnet) append(testNetParam)
        }
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        val url = buildString {
            append(baseUrl)
            append("tx/$transactionHash")
            if (isTestnet) append(testNetParam)
        }
        return TxExploreState.Url(url)
    }
}