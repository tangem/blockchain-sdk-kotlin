package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoNetworkProvider
import com.tangem.blockchain.blockchains.ravencoin.network.RavencoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProvider
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider

internal fun Blockchain.getBitcoinNetworkProviders(
    blockchain: Blockchain,
    config: BlockchainSdkConfig,
): List<BitcoinNetworkProvider> {
    return when (this) {
        Blockchain.Bitcoin -> listOfNotNull(
            getNowNodesProvider(blockchain, config),
            getGetBlockProvider(blockchain, config),
            BlockchainInfoNetworkProvider(),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config)
        )
        Blockchain.BitcoinTestnet -> listOfNotNull(
            getNowNodesProvider(blockchain, config),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config)
        )
        Blockchain.Litecoin,
        Blockchain.Dogecoin,
        Blockchain.Dash,
        -> listOfNotNull(
            getNowNodesProvider(blockchain, config),
            getGetBlockProvider(blockchain, config),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config)
        )
        Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> listOfNotNull(
            *getBlockchairProviders(blockchain, config),
        )
        Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> if (blockchain.isTestnet()) {
            listOf("https://testnet.ravencoin.network/api/")
        } else {
            listOf(
                "https://api.ravencoin.org/api/",
                "https://ravencoin.network/api/",
            )
        }.map(::RavencoinNetworkProvider)
        else -> throw IllegalStateException("$this isn't supported")
    }
}

private fun getNowNodesProvider(
    blockchain: Blockchain,
    config: BlockchainSdkConfig,
): BitcoinNetworkProvider? {
    return if (config.nowNodeCredentials != null && config.nowNodeCredentials.apiKey.isNotBlank()) {
        BlockBookNetworkProvider(
            config = BlockBookConfig.NowNodes(nowNodesCredentials = config.nowNodeCredentials),
            blockchain = blockchain
        )
    } else {
        null
    }
}

private fun getGetBlockProvider(
    blockchain: Blockchain,
    config: BlockchainSdkConfig,
): BitcoinNetworkProvider? {
    return if (config.getBlockCredentials != null && config.getBlockCredentials.apiKey.isNotBlank()) {
        BlockBookNetworkProvider(
            config = BlockBookConfig.GetBlock(getBlockCredentials = config.getBlockCredentials),
            blockchain = blockchain
        )
    } else {
        null
    }
}

private fun getBlockchairProviders(
    blockchain: Blockchain,
    config: BlockchainSdkConfig,
): Array<BitcoinNetworkProvider> {
    return config.blockchairCredentials?.let { blockchairCredentials ->
        blockchairCredentials.apiKey.map { apiKey ->
            BlockchairNetworkProvider(
                blockchain = blockchain,
                apiKey = apiKey,
                authorizationToken = blockchairCredentials.authToken,
            )
        }.toTypedArray()
    } ?: emptyArray()
}

private fun getBlockcypherProvider(
    blockchain: Blockchain,
    config: BlockchainSdkConfig,
): BitcoinNetworkProvider? {
    return config.blockcypherTokens?.let {
        BlockcypherNetworkProvider(blockchain = blockchain, tokens = config.blockcypherTokens)
    }
}