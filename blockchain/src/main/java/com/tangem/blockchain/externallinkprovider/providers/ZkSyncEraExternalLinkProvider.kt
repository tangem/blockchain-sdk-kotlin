package com.tangem.blockchain.externallinkprovider.providers

import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset

internal class ZkSyncEraExternalLinkProvider(isTestnet: Boolean) : ExternalLinkProvider {

    override val testNetTopUpUrl: String = "https://faucet.chainstack.com/zksync-testnet-faucet/"

    override val explorerBaseUrl: String = if (isTestnet) {
        "https://sepolia.explorer.zksync.io/"
    } else {
        "https://explorer.zksync.io/"
    }

    override fun explorerUrl(walletAddress: String, contractAddress: String?): String {
        return "${explorerBaseUrl}address/$walletAddress"
    }

    override fun getExplorerTxUrl(transactionHash: String): TxExploreState {
        return TxExploreState.Url(url = "${explorerBaseUrl}tx/$transactionHash")
    }

    override fun getNFTExplorerUrl(assetIdentifier: NFTAsset.Identifier): String {
        require(assetIdentifier is NFTAsset.Identifier.EVM)
        // https://explorer.zksync.io/ doesn't provide a way to see specific nft asset
        return "https://zksync.blockscout.com/token/${assetIdentifier.tokenAddress}/instance/${assetIdentifier.tokenId}"
    }
}