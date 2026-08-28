package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EvmNetworkServiceFactory
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProviderFactory
import com.tangem.blockchain.tokenbalance.TokenBalanceEvmProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
) :
    WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            val multiJsonRpcProvider = EvmNetworkServiceFactory.createMultiProvider(
                blockchain = blockchain,
                providerTypes = input.providerTypes,
                config = input.config,
            )
            val networkProviderMap = input.providerTypes
                .zip(multiJsonRpcProvider.providers)
                .toMap()

            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiJsonRpcProvider)

            val pendingTransactionsProvider = PendingTransactionsProviderFactory(
                dataStorage = dataStorage,
            ).makeProvider(this, multiJsonRpcProvider, networkProviderMap)

            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = this),
                networkProvider = EvmNetworkServiceFactory.createNetworkService(
                    blockchain = blockchain,
                    multiNetworkProvider = multiJsonRpcProvider,
                    yieldSupplyProvider = yieldLendingProvider,
                    blockcypherNetworkProvider = BlockcypherNetworkProvider(
                        blockchain = blockchain,
                        tokens = input.config.blockcypherTokens,
                    ),
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                tokenBalanceProvider = TokenBalanceEvmProviderFactory.createTokenBalanceEvmProvider(
                    blockchain,
                    input.config,
                ),
                yieldSupplyProvider = yieldLendingProvider,
                supportsENS = true,
                pendingTransactionsProvider = pendingTransactionsProvider,
            )
        }
    }
}