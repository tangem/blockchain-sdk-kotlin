package com.tangem.blockchain.transactionhistory

import com.tangem.blockchain.blockchains.kaspa.KaspaProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.network.blockbook.config.DogecoinMockBlockBookConfig
import com.tangem.blockchain.network.blockbook.config.NowNodesConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.transactionhistory.blockchains.algorand.AlgorandTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.algorand.network.AlgorandIndexerApi
import com.tangem.blockchain.transactionhistory.blockchains.bitcoin.BitcoinTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.ethereum.EthereumTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.kaspa.KaspaTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.kaspa.network.KaspaApiService
import com.tangem.blockchain.transactionhistory.blockchains.polygon.EtherscanTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherScanApi
import com.tangem.blockchain.transactionhistory.blockchains.tron.TronTransactionHistoryProvider

internal object TransactionHistoryProviderFactory {

    fun makeProvider(blockchain: Blockchain, config: BlockchainSdkConfig): TransactionHistoryProvider {
        if (blockchain.isEtherscanCompatible()) {
            return createEtherscanProvider(blockchain, config)
        }
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
            Blockchain.EthereumPow,
            -> createEthereumProvider(blockchain, config)

            Blockchain.Algorand -> createAlgorandProvider(blockchain, config)

            Blockchain.Tron -> createTronProvider(blockchain, config)

            Blockchain.Koinos -> createKoinosProvider()

            Blockchain.Kaspa, Blockchain.KaspaTestnet -> createKaspaProvider(blockchain)

            else -> DefaultTransactionHistoryProvider
        }
    }

    fun makeDogecoinMockProvider(blockchain: Blockchain): TransactionHistoryProvider {
        return BitcoinTransactionHistoryProvider(
            blockchain = blockchain,
            blockBookApi = BlockBookApi(blockchain = blockchain, config = DogecoinMockBlockBookConfig()),
        )
    }

    private fun Blockchain.isEtherscanCompatible(): Boolean {
        return when (this) {
            Blockchain.ApeChain,
            Blockchain.Base,
            Blockchain.Blast,
            Blockchain.Gnosis,
            Blockchain.Hyperliquid,
            Blockchain.Mantle,
            Blockchain.Moonbeam,
            Blockchain.Moonriver,
            Blockchain.Optimism,
            Blockchain.PolygonZkEVM,
            Blockchain.Polygon,
            Blockchain.Scroll,
            Blockchain.Sonic,
            Blockchain.XDC,
            Blockchain.ZkSyncEra,
            Blockchain.BSC,
            -> true
            else -> false
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

    private fun createEtherscanProvider(
        blockchain: Blockchain,
        config: BlockchainSdkConfig,
    ): TransactionHistoryProvider {
        val apiKey = config.etherscanApiKey ?: return DefaultTransactionHistoryProvider
        return EtherscanTransactionHistoryProvider(
            blockchain = blockchain,
            api = createRetrofitInstance("https://api.etherscan.io/v2/").create(EtherScanApi::class.java),
            etherscanApiKey = apiKey,
        )
    }

    private fun createKaspaProvider(blockchain: Blockchain): KaspaTransactionHistoryProvider {
        val baseUrl = if (blockchain.isTestnet()) KaspaProvidersBuilder.TESTNET_URL else "https://api.kaspa.org/"
        return KaspaTransactionHistoryProvider(
            blockchain = blockchain,
            kaspaApiService = createRetrofitInstance(baseUrl = baseUrl).create(KaspaApiService::class.java),
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