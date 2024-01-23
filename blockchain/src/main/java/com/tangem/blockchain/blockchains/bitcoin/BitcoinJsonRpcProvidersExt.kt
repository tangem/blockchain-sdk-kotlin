package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.ravencoin.network.RavencoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProvider
import com.tangem.blockchain.network.blockbook.config.GetBlockConfig
import com.tangem.blockchain.network.blockbook.config.NowNodesConfig
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider

internal fun Blockchain.getBitcoinNetworkProviders(
    blockchain: Blockchain,
    config: BlockchainSdkConfig,
): List<BitcoinNetworkProvider> {
    return when (this) {
        Blockchain.Bitcoin -> listOfNotNull(
            getNowNodesProvider(blockchain, config),
            getGetBlockProvider(blockchain, config.getBlockCredentials),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config),
        )
        Blockchain.BitcoinTestnet -> listOfNotNull(
            getNowNodesProvider(blockchain, config),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config),
        )
        Blockchain.Litecoin,
        Blockchain.Dogecoin,
        Blockchain.Dash,
        -> listOfNotNull(
            getNowNodesProvider(blockchain, config),
            getGetBlockProvider(blockchain, config.getBlockCredentials),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config),
        )
        Blockchain.BitcoinCash -> listOfNotNull(
            *getBlockchairProviders(blockchain, config),
        )
        // TODO: we don't have BCH testnet providers now. Maybe remove it completely?
        Blockchain.BitcoinCashTestnet -> error("No providers for $this")
        Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> if (blockchain.isTestnet()) {
            listOf("https://testnet.ravencoin.network/api/")
        } else {
            listOf(
                "https://api.ravencoin.org/api/",
                "https://explorer.rvn.zelcore.io/api/",
            )
        }.map(::RavencoinNetworkProvider)
        else -> error("$this isn't supported")
    }
}

private fun getNowNodesProvider(blockchain: Blockchain, config: BlockchainSdkConfig): BitcoinNetworkProvider? {
    return if (config.nowNodeCredentials != null && config.nowNodeCredentials.apiKey.isNotBlank()) {
        BlockBookNetworkProvider(
            config = NowNodesConfig(nowNodesCredentials = config.nowNodeCredentials),
            blockchain = blockchain,
        )
    } else {
        null
    }
}

private fun getGetBlockProvider(
    blockchain: Blockchain,
    getBlockCredentials: GetBlockCredentials?,
): BitcoinNetworkProvider? {
    val credentials = getBlockCredentials ?: return null
    val accessToken = when (blockchain) {
        Blockchain.Bitcoin -> credentials.bitcoin
        Blockchain.Dash -> credentials.dash
        Blockchain.Dogecoin -> credentials.dogecoin
        Blockchain.Litecoin -> credentials.litecoin
        else -> null
    } ?: return null

    return if (!accessToken.blockBookRest.isNullOrEmpty() && !accessToken.jsonRpc.isNullOrEmpty()) {
        BlockBookNetworkProvider(
            config = GetBlockConfig(
                blockBookToken = accessToken.blockBookRest,
                jsonRpcToken = accessToken.jsonRpc,
            ),
            blockchain = blockchain,
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

private fun getBlockcypherProvider(blockchain: Blockchain, config: BlockchainSdkConfig): BitcoinNetworkProvider? {
    return config.blockcypherTokens?.let {
        BlockcypherNetworkProvider(blockchain = blockchain, tokens = config.blockcypherTokens)
    }
}
