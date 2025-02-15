package com.tangem.blockchain.nft

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.providers.moralis.MoralisEvmNFTProvider
import com.tangem.blockchain.nft.providers.nftscan.NFTScanTonNFTProvider

class NFTProviderFactory(
    private val config: NFTSdkConfig = NFTSdkConfig(),
) {
    fun createNFTProvider(blockchain: Blockchain): NFTProvider = when (blockchain) {
        Blockchain.Ethereum, // supported testnet - Sepolia (11155111)
        Blockchain.Arbitrum, // supported testnet - Sepolia (421614)
        Blockchain.Avalanche,
        Blockchain.Fantom, Blockchain.FantomTestnet,
        Blockchain.BSC, Blockchain.BSCTestnet,
        Blockchain.Polygon, // supported testnet - Amoy (80002)
        Blockchain.Gnosis,
        Blockchain.Cronos,
        Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet,
        Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
        Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet,
        Blockchain.Moonriver, Blockchain.MoonriverTestnet,
        Blockchain.Chiliz, Blockchain.ChilizTestnet,
        Blockchain.Mantle, // supported testnet - Sepolia (5003)
        Blockchain.Optimism, // supported testnet - Sepolia (11155420)
        Blockchain.Base, Blockchain.BaseTestnet,
        Blockchain.Blast, Blockchain.BlastTestnet,
        -> MoralisEvmNFTProvider(
            blockchain = blockchain,
            apiKey = config.moralisApiKey,
        )

        Blockchain.TON,
        -> NFTScanTonNFTProvider(
            apiKey = config.nftScanApiKey,
        )

        else -> NFTEmptyProvider()
    }
}