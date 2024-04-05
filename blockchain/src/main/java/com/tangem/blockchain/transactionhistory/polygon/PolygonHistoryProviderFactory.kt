package com.tangem.blockchain.transactionhistory.polygon

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.transactionhistory.polygon.network.PolygonScanApi

internal class PolygonHistoryProviderFactory : TransactionHistoryProviderFactory {
    override fun makeProvider(config: BlockchainSdkConfig, blockchain: Blockchain): TransactionHistoryProvider? {
        return config.polygonScanApiKey?.let { apiKey ->
            val suffix = if (blockchain.isTestnet()) "api-testnet" else "api"
            PolygonTransactionHistoryProvider(
                blockchain = blockchain,
                api = createRetrofitInstance("https://$suffix.polygonscan.com/").create(PolygonScanApi::class.java),
                polygonScanApiKey = apiKey,
            )
        }
    }
}