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
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumWalletManagerAssembly(private val dataStorage: AdvancedDataStorage) :
    WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            val multiJsonRpcProvider = MultiNetworkProvider(
                providers = EthereumProvidersBuilder(
                    providerTypes = input.providerTypes,
                    config = input.config,
                ).build(blockchain),
            )

            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiJsonRpcProvider)

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
            )
        }
    }
}