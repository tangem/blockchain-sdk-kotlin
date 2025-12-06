package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.BaseProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.BlastProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CyberProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.InkProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.MantaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumLegacyTransactionBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumOptimisticRollupWalletManagerAssembly(private val dataStorage: AdvancedDataStorage) :
    WalletManagerAssembly<EthereumOptimisticRollupWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumOptimisticRollupWalletManager {
        with(input.wallet) {
            android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating wallet manager for blockchain: $blockchain")
            android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Provider types: ${input.providerTypes}")
            
            val providersBuilder = getProvidersBuilder(
                blockchain = blockchain,
                providerTypes = input.providerTypes,
                config = input.config,
            )
            android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Providers builder created: ${providersBuilder::class.simpleName}")
            
            val providers = providersBuilder.build(blockchain)
            android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Providers built: ${providers.size} providers")
            
            val multiNetworkProvider = MultiNetworkProvider(providers)
            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

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
            )
        }
    }

    private fun getProvidersBuilder(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): NetworkProvidersBuilder<EthereumJsonRpcProvider> {
        android.util.Log.d("EthereumOptimisticRollupWMAssembly", "getProvidersBuilder called for: $blockchain")
        
        return when (blockchain) {
            Blockchain.Optimism, Blockchain.OptimismTestnet -> {
                android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating OptimismProvidersBuilder")
                OptimismProvidersBuilder(providerTypes, config)
            }
            Blockchain.Base, Blockchain.BaseTestnet -> {
                android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating BaseProvidersBuilder")
                BaseProvidersBuilder(providerTypes, config)
            }
            Blockchain.Manta, Blockchain.MantaTestnet -> {
                android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating MantaProvidersBuilder")
                MantaProvidersBuilder(providerTypes)
            }
            Blockchain.Blast, Blockchain.BlastTestnet -> {
                android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating BlastProvidersBuilder")
                BlastProvidersBuilder(providerTypes, config)
            }
            Blockchain.Cyber, Blockchain.CyberTestnet -> {
                android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating CyberProvidersBuilder")
                CyberProvidersBuilder(providerTypes)
            }
            Blockchain.Ink, Blockchain.InkTestnet -> {
                android.util.Log.d("EthereumOptimisticRollupWMAssembly", "Creating InkProvidersBuilder")
                InkProvidersBuilder(providerTypes, config)
            }
            else -> {
                android.util.Log.e("EthereumOptimisticRollupWMAssembly", "Unsupported blockchain: $blockchain")
                error("Unsupported blockchain: $blockchain")
            }
        }
    }
}