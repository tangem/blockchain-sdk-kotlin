package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EvmNetworkServiceFactory
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.L1GasOracleConfig
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProviderFactory
import com.tangem.blockchain.tokenbalance.TokenBalanceEvmProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumOptimisticRollupWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
) :
    WalletManagerAssembly<EthereumOptimisticRollupWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumOptimisticRollupWalletManager {
        with(input.wallet) {
            val multiNetworkProvider = EvmNetworkServiceFactory.createMultiProvider(
                blockchain = blockchain,
                providerTypes = input.providerTypes,
                config = input.config,
            )
            val networkProviderMap = input.providerTypes
                .zip(multiNetworkProvider.providers)
                .toMap()

            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

            val pendingTransactionsProvider = PendingTransactionsProviderFactory(
                dataStorage = dataStorage,
            ).makeProvider(
                wallet = this,
                networkProvider = multiNetworkProvider,
                networkProviderMap = networkProviderMap,
            )

            return EthereumOptimisticRollupWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = this),
                networkProvider = EvmNetworkServiceFactory.createNetworkService(
                    blockchain = blockchain,
                    multiNetworkProvider = multiNetworkProvider,
                    yieldSupplyProvider = yieldLendingProvider,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                tokenBalanceProvider = TokenBalanceEvmProviderFactory.createTokenBalanceEvmProvider(
                    blockchain,
                    input.config,
                ),
                yieldSupplyProvider = yieldLendingProvider,
                pendingTransactionsProvider = pendingTransactionsProvider,
                l1GasOracleConfig = getL1GasOracleConfig(blockchain),
            )
        }
    }

    private fun getL1GasOracleConfig(blockchain: Blockchain): L1GasOracleConfig {
        return when (blockchain) {
            Blockchain.Optimism, Blockchain.OptimismTestnet,
            Blockchain.Base, Blockchain.BaseTestnet,
            Blockchain.Manta, Blockchain.MantaTestnet,
            Blockchain.Blast, Blockchain.BlastTestnet,
            Blockchain.Cyber, Blockchain.CyberTestnet,
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
    }
}