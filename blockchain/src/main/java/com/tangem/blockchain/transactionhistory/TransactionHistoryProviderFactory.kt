package com.tangem.blockchain.transactionhistory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.blockbook.config.NowNodesConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.transactionhistory.blockchains.algorand.AlgorandTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.algorand.network.AlgorandIndexerApi
import com.tangem.blockchain.transactionhistory.blockchains.bitcoin.BitcoinTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.ethereum.EthereumTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.polygon.PolygonTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonScanApi
import com.tangem.blockchain.transactionhistory.blockchains.tron.TronTransactionHistoryProvider

internal object TransactionHistoryProviderFactory {

    fun makeProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TransactionHistoryProvider {
        return when (blockchain) {
            Blockchain.Bitcoin,
            Blockchain.Litecoin,
            Blockchain.Dogecoin,
            Blockchain.Dash,
            Blockchain.BitcoinCash,
            -> createBitcoinProvider(blockchain, config)

            Blockchain.Ethereum,
            Blockchain.EthereumClassic,
            Blockchain.Arbitrum,
            Blockchain.Avalanche,
            Blockchain.BSC,
            Blockchain.EthereumPow,
            -> createEthereumProvider(blockchain, config)

            Blockchain.Algorand -> createAlgorandProvider(blockchain, config)

            Blockchain.Tron -> createTronProvider(blockchain, config)

            Blockchain.Polygon -> createPolygonProvider(blockchain, config)

            Blockchain.Koinos -> createKoinosProvider()

            else -> DefaultTransactionHistoryProvider
        }
    }

    private fun createBitcoinProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TransactionHistoryProvider {
        val blockBookApi =
            createBlockBookApiWithNowNodesConfig(blockchain, config) ?: return DefaultTransactionHistoryProvider
        return BitcoinTransactionHistoryProvider(blockchain = blockchain, blockBookApi = blockBookApi)
    }

    private fun createEthereumProvider(
        blockchain: Blockchain,
        config: BlockchainSdkConfig,
    ): TransactionHistoryProvider {
        val blockBookApi =
            createBlockBookApiWithNowNodesConfig(blockchain, config) ?: return DefaultTransactionHistoryProvider
        return EthereumTransactionHistoryProvider(blockchain = blockchain, blockBookApi = blockBookApi)
    }

    private fun createAlgorandProvider(
        blockchain: Blockchain,
        config: BlockchainSdkConfig,
    ): TransactionHistoryProvider {
        val nowNodesApiKey = config.nowNodeCredentials?.apiKey ?: return DefaultTransactionHistoryProvider
        return AlgorandTransactionHistoryProvider(
            blockchain = blockchain,
            algorandIndexerApi = createRetrofitInstance(
                baseUrl = "https://algo-index.nownodes.io/$nowNodesApiKey/",
            ).create(AlgorandIndexerApi::class.java),
        )
    }

    private fun createTronProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TransactionHistoryProvider {
        val blockBookApi =
            createBlockBookApiWithNowNodesConfig(blockchain, config) ?: return DefaultTransactionHistoryProvider
        return TronTransactionHistoryProvider(blockchain = blockchain, blockBookApi = blockBookApi)
    }

    private fun createPolygonProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TransactionHistoryProvider {
        val apiKey = config.polygonScanApiKey ?: return DefaultTransactionHistoryProvider
        return PolygonTransactionHistoryProvider(
            blockchain = blockchain,
            api = createRetrofitInstance("https://api.polygonscan.com/").create(PolygonScanApi::class.java),
            polygonScanApiKey = apiKey,
        )
    }

    /**
     * No implementation for now.
     * See [com.tangem.blockchain.transactionhistory.blockchains.koinos.KoinosTransactionHistoryProvider] deprecation doc.
     */
    private fun createKoinosProvider(): TransactionHistoryProvider {
        return DefaultTransactionHistoryProvider
    }

    private fun createBlockBookApiWithNowNodesConfig(
        blockchain: Blockchain,
        config: BlockchainSdkConfig,
    ): BlockBookApi? {
        val nowNodesConfig = createNowNodesConfig(config) ?: return null
        return BlockBookApi(config = nowNodesConfig, blockchain = blockchain)
    }

    private fun createNowNodesConfig(config: BlockchainSdkConfig): NowNodesConfig? {
        return config.nowNodeCredentials?.let(::NowNodesConfig)
    }
}