package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.blockchains.algorand.AlgorandTransactionHistoryProvider
import com.tangem.blockchain.blockchains.algorand.network.AlgorandIndexerApi
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionHistoryProvider
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionHistoryProvider
import com.tangem.blockchain.blockchains.tron.TronTransactionHistoryProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.blockbook.config.NowNodesConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.createRetrofitInstance

internal fun Blockchain.getTransactionHistoryProvider(config: BlockchainSdkConfig): TransactionHistoryProvider {
    return if (config.nowNodeCredentials != null && config.nowNodeCredentials.apiKey.isNotBlank()) {
        when (this) {
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Litecoin,
            Blockchain.Dogecoin,
            Blockchain.Dash,
            Blockchain.BitcoinCash,
            -> BitcoinTransactionHistoryProvider(
                blockchain = this,
                blockBookApi = BlockBookApi(
                    config = NowNodesConfig(nowNodesCredentials = config.nowNodeCredentials),
                    blockchain = this,
                ),
            )

            Blockchain.Ethereum,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumClassic,
            Blockchain.Arbitrum,
            Blockchain.Avalanche,
            Blockchain.BSC,
            // Blockchain.Polygon,
            Blockchain.EthereumPow,
            // Blockchain.Kava,
            -> EthereumTransactionHistoryProvider(
                blockchain = this,
                blockBookApi = BlockBookApi(
                    config = NowNodesConfig(nowNodesCredentials = config.nowNodeCredentials),
                    blockchain = this,
                ),
            )

            Blockchain.Algorand -> AlgorandTransactionHistoryProvider(
                blockchain = this,
                algorandIndexerApi = createRetrofitInstance(
                    baseUrl = "https://algo-index.nownodes.io/${config.nowNodeCredentials.apiKey}/",
                ).create(AlgorandIndexerApi::class.java),
            )

            Blockchain.Tron -> {
                TronTransactionHistoryProvider(
                    blockchain = this,
                    blockBookApi = BlockBookApi(
                        config = NowNodesConfig(nowNodesCredentials = config.nowNodeCredentials),
                        blockchain = this,
                    ),
                )
            }

            else -> DefaultTransactionHistoryProvider
        }
    } else {
        DefaultTransactionHistoryProvider
    }
}
