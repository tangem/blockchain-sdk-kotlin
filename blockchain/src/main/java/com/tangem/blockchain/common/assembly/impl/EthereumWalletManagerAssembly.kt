package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory
import kotlinx.coroutines.CoroutineScope

internal class EthereumWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
    private val coroutineScope: CoroutineScope,
) :
    WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            val multiJsonRpcProvider = MultiNetworkProvider(
                providers = EthereumProvidersBuilder(
                    providerTypes = input.providerTypes,
                    config = input.config,
                ).build(blockchain),
                blockchain = blockchain,
            )
            val networkProviderMap = input.providerTypes
                .zip(multiJsonRpcProvider.providers)
                .toMap()

            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiJsonRpcProvider)

            val pendingTransactionsProvider = PendingTransactionsProviderFactory(
                dataStorage = dataStorage,
                coroutineScope = coroutineScope,
            ).makeProvider(this, multiJsonRpcProvider, networkProviderMap)

            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = this),
                networkProvider = EthereumNetworkService(
                    multiJsonRpcProvider = multiJsonRpcProvider,
                    blockcypherNetworkProvider = BlockcypherNetworkProvider(
                        blockchain = blockchain,
                        tokens = input.config.blockcypherTokens,
                    ),
                    yieldSupplyProvider = yieldLendingProvider,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                yieldSupplyProvider = yieldLendingProvider,
                supportsENS = true,
                pendingTransactionsProvider = pendingTransactionsProvider,
            )
        }
    }
}