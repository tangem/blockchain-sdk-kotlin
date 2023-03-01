package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider

internal fun Blockchain.getBitcoinNetworkProviders(
    blockchain: Blockchain,
    config: BlockchainSdkConfig
): List<BitcoinNetworkProvider> {
    return when (this) {
        Blockchain.Bitcoin -> listOfNotNull(
            // FIXME("will be included in version 4.2")
            // getNowNodesProvider(blockchain, config),
            // getGetBlockProvider(blockchain, config),
            BlockchainInfoNetworkProvider(),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config)
        )
        Blockchain.BitcoinTestnet -> listOfNotNull(
            // FIXME("will be included in version 4.2")
            // getNowNodesProvider(blockchain, config),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config)
        )
        Blockchain.Litecoin,
        Blockchain.Dogecoin,
        Blockchain.Dash -> listOfNotNull(
            // FIXME("will be included in version 4.2")
            // getNowNodesProvider(blockchain, config),
            // getGetBlockProvider(blockchain, config),
            *getBlockchairProviders(blockchain, config),
            getBlockcypherProvider(blockchain, config)
        )
        Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> listOfNotNull(
            *getBlockchairProviders(blockchain, config),
        )
        else -> throw IllegalStateException("$this isn't supported")
    }
}

// FIXME("will be included in version 4.2")
// private fun getNowNodesProvider(
//     blockchain: Blockchain,
//     config: BlockchainSdkConfig
// ): BitcoinNetworkProvider? {
//     return config.nowNodeCredentials?.let { credentials ->
//         BlockBookNetworkProvider(
//             config = BlockBookConfig.NowNodes(nowNodesCredentials = credentials),
//             blockchain = blockchain
//         )
//     }
// }
//
// private fun getGetBlockProvider(
//     blockchain: Blockchain,
//     config: BlockchainSdkConfig
// ): BitcoinNetworkProvider? {
//     return config.getBlockCredentials?.let { credentials ->
//         BlockBookNetworkProvider(
//             config = BlockBookConfig.GetBlock(getBlockCredentials = credentials),
//             blockchain = blockchain
//         )
//     }
// }

private fun getBlockchairProviders(
    blockchain: Blockchain,
    config: BlockchainSdkConfig
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
    config: BlockchainSdkConfig
): BitcoinNetworkProvider? {
    return config.blockcypherTokens?.let {
        BlockcypherNetworkProvider(blockchain = blockchain, tokens = config.blockcypherTokens)
    }
}