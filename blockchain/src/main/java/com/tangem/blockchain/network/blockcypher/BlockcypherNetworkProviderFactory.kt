package com.tangem.blockchain.network.blockcypher

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class BlockcypherNetworkProviderFactory(
    private val config: BlockchainSdkConfig,
) {

    fun create(blockchain: Blockchain): BitcoinNetworkProvider? {
        return config.blockcypherTokens?.let {
            BlockcypherNetworkProvider(blockchain = blockchain, tokens = config.blockcypherTokens)
        }
    }
}