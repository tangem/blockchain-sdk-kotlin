package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.BaseProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.BlastProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CyberProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.MantaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.ScrollProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.L1GasOracleConfig
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
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumOptimisticRollupWalletManagerAssembly(private val dataStorage: AdvancedDataStorage) :
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
            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

            return EthereumOptimisticRollupWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = this),
                networkProvider = EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider,
                    yieldSupplyProvider = yieldLendingProvider,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                yieldSupplyProvider = yieldLendingProvider,
                l1GasOracleConfig = getL1GasOracleConfig(blockchain),
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
            Blockchain.Scroll, Blockchain.ScrollTestnet -> ScrollProvidersBuilder(providerTypes, config)
            else -> error("Unsupported blockchain: $blockchain")
        }
    }

    private fun getL1GasOracleConfig(blockchain: Blockchain): L1GasOracleConfig {
        return when (blockchain) {
            Blockchain.Scroll, Blockchain.ScrollTestnet -> L1GasOracleConfig(
                address = SCROLL_L1_GAS_ORACLE_ADDRESS,
                feeMultiplier = SCROLL_FEE_MULTIPLIER,
            )
            Blockchain.Optimism, Blockchain.OptimismTestnet,
            Blockchain.Base, Blockchain.BaseTestnet,
            Blockchain.Manta, Blockchain.MantaTestnet,
            Blockchain.Blast, Blockchain.BlastTestnet,
            Blockchain.Cyber, Blockchain.CyberTestnet,
            Blockchain.Plasma, Blockchain.PlasmaTestnet,
            -> L1GasOracleConfig(
                address = OPTIMISM_L1_GAS_ORACLE_ADDRESS,
                feeMultiplier = OPTIMISM_FEE_MULTIPLIER,
            )
            else -> error("Unsupported blockchain: $blockchain")
        }
    }

    companion object {
        private const val OPTIMISM_L1_GAS_ORACLE_ADDRESS = "0x420000000000000000000000000000000000000F"
        private const val OPTIMISM_FEE_MULTIPLIER = 1.1

        private const val SCROLL_L1_GAS_ORACLE_ADDRESS = "0x5300000000000000000000000000000000000002"
        private const val SCROLL_FEE_MULTIPLIER = 3.0
    }
}