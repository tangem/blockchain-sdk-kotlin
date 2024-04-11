package com.tangem.blockchain.transactionhistory.koinos

import com.tangem.blockchain.blockchains.koinos.KoinosProviderBuilder
import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal class KoinosHistoryProviderFactory : TransactionHistoryProviderFactory {
    override fun makeProvider(config: BlockchainSdkConfig, blockchain: Blockchain): TransactionHistoryProvider {
        return KoinosTransactionHistoryProvider(
            networkService = KoinosNetworkService(
                KoinosProviderBuilder().build(blockchain),
            ),
        )
    }
}