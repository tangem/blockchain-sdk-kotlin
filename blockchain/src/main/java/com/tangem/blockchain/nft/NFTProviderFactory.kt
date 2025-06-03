package com.tangem.blockchain.nft

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.nft.providers.moralis.MoralisEvmNFTProvider
import com.tangem.blockchain.nft.providers.moralis.MoralisSolanaNFTProvider

internal object NFTProviderFactory {
    fun createNFTProvider(blockchain: Blockchain, config: BlockchainSdkConfig): NFTProvider = when {
        blockchain.canHandleNFTs() -> {
            when {
                blockchain.isEvm() -> MoralisEvmNFTProvider(
                    blockchain = blockchain,
                    apiKey = config.moralisApiKey,
                )
                blockchain == Blockchain.Solana -> MoralisSolanaNFTProvider(
                    blockchain = blockchain,
                    apiKey = config.moralisApiKey,
                )
                else -> DefaultNFTProvider
            }
        }
        else -> DefaultNFTProvider
    }
}