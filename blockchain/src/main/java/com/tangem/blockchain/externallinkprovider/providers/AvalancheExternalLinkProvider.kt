package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset

internal class AvalancheExternalLinkProvider(private val isTestnet: Boolean) : ExternalLinkProvider {

    override val explorerBaseUrl: String =
        if (isTestnet) "https://testnet.avascan.info/blockchain/c/" else "https://subnets.avax.network/c-chain/"

    override val testNetTopUpUrl: String? = if (isTestnet) "https://core.app/tools/testnet-faucet/" else null

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return if (isTestnet && contractAddress != null) {
            explorerBaseUrl + "token/$contractAddress?a=$walletAddress"
        } else {
            explorerBaseUrl + "address/$walletAddress"
        }
    }

    override fun getExplorerTxUrl(transactionHash: String, contractAddress: String?): TxExploreState {
        return TxExploreState.Url(explorerBaseUrl + "tx/$transactionHash")
    }

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        val type = assetIdentifier.contractType.value.lowercase()
        val tokenAddress = assetIdentifier.tokenAddress
        val tokenId = assetIdentifier.tokenId
        // https://subnets.avax.network/c-chain/ doesn't provide a way to see specific nft asset
        return "https://avascan.info/blockchain/c/$type/$tokenAddress/nft/$tokenId"
    }
}