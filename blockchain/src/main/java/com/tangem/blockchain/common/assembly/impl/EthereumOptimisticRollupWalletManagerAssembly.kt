package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.BaseProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.BlastProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CyberProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.MantaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumLegacyTransactionBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismProvidersBuilder
import com.tangem.blockchain.blockchains.plasma.PlasmaProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory
import kotlinx.coroutines.CoroutineScope

internal class EthereumOptimisticRollupWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
    private val coroutineScope: CoroutineScope,
) :
    WalletManagerAssembly<EthereumOptimisticRollupWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumOptimisticRollupWalletManager {
        with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                providers = getProvidersBuilder(
                    blockchain = blockchain,
                    providerTypes = input.providerTypes,
                    config = input.config,
                ).build(blockchain),
                blockchain = blockchain,
            )
            val networkProviderMap = input.providerTypes
                .zip(multiNetworkProvider.providers)
                .toMap()

            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

            val pendingTransactionsProvider = PendingTransactionsProviderFactory(
                dataStorage = dataStorage,
                coroutineScope = coroutineScope,
            ).makeProvider(
                wallet = this,
                networkProvider = multiNetworkProvider,
                networkProviderMap = networkProviderMap,
            )

            return EthereumOptimisticRollupWalletManager(
                wallet = this,
                // TODO: [REDACTED_JIRA]
                transactionBuilder = EthereumLegacyTransactionBuilder(wallet = this),
                networkProvider = EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider,
                    yieldSupplyProvider = yieldLendingProvider,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                yieldSupplyProvider = yieldLendingProvider,
                pendingTransactionsProvider = pendingTransactionsProvider,
            )
        }
    }

    private fun getProvidersBuilder(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): NetworkProvidersBuilder<EthereumJsonRpcProvider> {
        return when (blockchain) {
            Blockchain.Optimism, Blockchain.OptimismTestnet -> OptimismProvidersBuilder(providerTypes, config)
            Blockchain.Base, Blockchain.BaseTestnet -> BaseProvidersBuilder(providerTypes, config)
            Blockchain.Manta, Blockchain.MantaTestnet -> MantaProvidersBuilder(providerTypes)
            Blockchain.Blast, Blockchain.BlastTestnet -> BlastProvidersBuilder(providerTypes, config)
            Blockchain.Cyber, Blockchain.CyberTestnet -> CyberProvidersBuilder(providerTypes)
            Blockchain.Plasma, Blockchain.PlasmaTestnet -> PlasmaProvidersBuilder(providerTypes, config)
            else -> error("Unsupported blockchain: $blockchain")
        }
    }
}