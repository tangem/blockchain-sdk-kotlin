package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.txhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi

internal fun Blockchain.getEthereumTransactionHistoryProvider(
    config: BlockchainSdkConfig,
): TransactionHistoryProvider {
    return when (this) {
        Blockchain.Ethereum,
        Blockchain.EthereumTestnet,
        Blockchain.Arbitrum,
        Blockchain.Avalanche,
        Blockchain.BSC,
        Blockchain.Polygon,
        Blockchain.EthereumPow,
        Blockchain.Kava,
        -> {
            if (config.nowNodeCredentials != null && config.nowNodeCredentials.apiKey.isNotBlank()) {
                EthereumTransactionHistoryProvider(
                    blockchain = this,
                    BlockBookApi(
                        config = BlockBookConfig.NowNodes(nowNodesCredentials = config.nowNodeCredentials),
                        blockchain = this,
                    )
                )
            } else {
                DefaultTransactionHistoryProvider
            }
        }
        else -> DefaultTransactionHistoryProvider
    }
}