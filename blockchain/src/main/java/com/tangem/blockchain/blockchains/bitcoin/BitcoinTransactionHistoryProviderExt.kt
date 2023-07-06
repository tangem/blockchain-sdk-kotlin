package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.txhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi

internal fun Blockchain.getBitcoinTransactionHistoryProvider(
    config: BlockchainSdkConfig,
): TransactionHistoryProvider {
    return when (this) {
        Blockchain.Bitcoin,
        Blockchain.BitcoinTestnet,
        -> {
            if (config.nowNodeCredentials != null && config.nowNodeCredentials.apiKey.isNotBlank()) {
                BitcoinTransactionHistoryProvider(
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
