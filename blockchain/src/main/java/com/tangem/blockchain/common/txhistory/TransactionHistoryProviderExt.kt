package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionHistoryProvider
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionHistoryProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi

internal fun Blockchain.getTransactionHistoryProvider(
    config: BlockchainSdkConfig,
): TransactionHistoryProvider {
    return if (config.nowNodeCredentials != null && config.nowNodeCredentials.apiKey.isNotBlank()) {
        when (this) {
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            // Blockchain.Litecoin,
            // Blockchain.Dogecoin,
            // Blockchain.Dash,
            -> {
                BitcoinTransactionHistoryProvider(
                    blockchain = this,
                    BlockBookApi(
                        config = BlockBookConfig.NowNodes(nowNodesCredentials = config.nowNodeCredentials),
                        blockchain = this,
                    )
                )
            }

            Blockchain.Ethereum,
            Blockchain.EthereumTestnet,
            // Blockchain.EthereumClassic,
            Blockchain.Arbitrum,
            Blockchain.Avalanche,
            Blockchain.BSC,
            // Blockchain.Polygon,
            Blockchain.EthereumPow,
            // Blockchain.Kava,
            -> {
                EthereumTransactionHistoryProvider(
                    blockchain = this,
                    BlockBookApi(
                        config = BlockBookConfig.NowNodes(nowNodesCredentials = config.nowNodeCredentials),
                        blockchain = this,
                    )
                )
            }

            // Blockchain.Tron -> {
            //     TronTransactionHistoryProvider(
            //         blockchain = this,
            //         BlockBookApi(
            //             config = BlockBookConfig.NowNodes(nowNodesCredentials = config.nowNodeCredentials),
            //             blockchain = this,
            //         )
            //     )
            // }

            else -> DefaultTransactionHistoryProvider
        }
    } else {
        DefaultTransactionHistoryProvider
    }
}
