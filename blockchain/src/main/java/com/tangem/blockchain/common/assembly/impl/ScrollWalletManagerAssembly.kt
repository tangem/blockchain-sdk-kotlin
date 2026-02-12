package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.ScrollProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.L1GasOracleConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class ScrollWalletManagerAssembly(private val dataStorage: AdvancedDataStorage) :
    WalletManagerAssembly<EthereumOptimisticRollupWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumOptimisticRollupWalletManager {
        with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                providers = ScrollProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                blockchain = blockchain,
            )
            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

            val networkProviderMap = input.providerTypes
                .zip(multiNetworkProvider.providers)
                .toMap()

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
                networkProvider = EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider,
                    yieldSupplyProvider = yieldLendingProvider,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                yieldSupplyProvider = yieldLendingProvider,
                pendingTransactionsProvider = pendingTransactionsProvider,
                l1GasOracleConfig = L1GasOracleConfig(
                    address = SCROLL_L1_GAS_ORACLE_ADDRESS,
                    feeMultiplier = SCROLL_FEE_MULTIPLIER,
                ),
            )
        }
    }

    companion object {
        private const val SCROLL_L1_GAS_ORACLE_ADDRESS = "0x5300000000000000000000000000000000000002"
        private const val SCROLL_FEE_MULTIPLIER = 3.0
    }
}