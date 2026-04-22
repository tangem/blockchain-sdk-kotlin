package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.SolanaProvidersBuilder
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.tokenbalance.providers.solana.SolanaTokenBalanceProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object SolanaWalletManagerAssembly : WalletManagerAssembly<SolanaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): SolanaWalletManager {
        return with(input.wallet) {
            val rpcClients = SolanaProvidersBuilder(input.providerTypes, input.config).build(blockchain)
            val networkServices = rpcClients.map { SolanaNetworkService(it) }
            val multiNetworkProvider = MultiNetworkProvider(networkServices, blockchain)

            SolanaWalletManager(
                wallet = this,
                providers = rpcClients,
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                tokenBalanceProvider = SolanaTokenBalanceProvider(multiNetworkProvider),
            )
        }
    }
}