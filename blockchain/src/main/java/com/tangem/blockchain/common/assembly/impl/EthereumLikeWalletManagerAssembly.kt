package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumLikeJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EvmNetworkServiceFactory
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.quai.QuaiWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.tokenbalance.TokenBalanceEvmProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProvider
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumLikeWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
) : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            val multiNetworkProvider = EvmNetworkServiceFactory.createMultiProvider(
                blockchain = blockchain,
                providerTypes = input.providerTypes,
                config = input.config,
            )
            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

            val networkProviderMap = input.providerTypes
                .zip(multiNetworkProvider.providers)
                .toMap()

            val pendingTransactionsProvider = PendingTransactionsProviderFactory(
                dataStorage = dataStorage,
            ).makeProvider(this, multiNetworkProvider, networkProviderMap)

            return createWalletManager(
                input = input,
                yieldLendingProvider = yieldLendingProvider,
                multiNetworkProvider = multiNetworkProvider,
                pendingTransactionsProvider = pendingTransactionsProvider,
            )
        }
    }

    private fun createWalletManager(
        input: WalletManagerAssemblyInput,
        yieldLendingProvider: YieldSupplyProvider,
        multiNetworkProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
        pendingTransactionsProvider: PendingTransactionsProvider,
    ): EthereumWalletManager {
        with(input.wallet) {
            return when (blockchain) {
                Blockchain.Quai, Blockchain.QuaiTestnet -> {
                    QuaiWalletManager(
                        wallet = this,
                        transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                        networkProvider = EvmNetworkServiceFactory.createNetworkService(
                            blockchain = blockchain,
                            multiNetworkProvider = multiNetworkProvider,
                            yieldSupplyProvider = yieldLendingProvider,
                        ),
                        transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(
                            blockchain = blockchain,
                            config = input.config,
                        ),
                        nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                        tokenBalanceProvider = TokenBalanceEvmProviderFactory.createTokenBalanceEvmProvider(
                            blockchain,
                            input.config,
                        ),
                        yieldSupplyProvider = yieldLendingProvider,
                        supportsENS = false,
                        pendingTransactionsProvider = pendingTransactionsProvider,
                    )
                }
                else -> {
                    EthereumWalletManager(
                        wallet = this,
                        transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                        networkProvider = EvmNetworkServiceFactory.createNetworkService(
                            blockchain = blockchain,
                            multiNetworkProvider = multiNetworkProvider,
                            yieldSupplyProvider = yieldLendingProvider,
                        ),
                        transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(
                            blockchain = blockchain,
                            config = input.config,
                        ),
                        nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                        tokenBalanceProvider = TokenBalanceEvmProviderFactory.createTokenBalanceEvmProvider(
                            blockchain,
                            input.config,
                        ),
                        yieldSupplyProvider = yieldLendingProvider,
                        supportsENS = false,
                        pendingTransactionsProvider = pendingTransactionsProvider,
                    )
                }
            }
        }
    }
}