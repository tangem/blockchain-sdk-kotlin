package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismProvidersBuilder
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object EthereumOptimisticRollupWalletManagerAssembly :
    WalletManagerAssembly<EthereumOptimisticRollupWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumOptimisticRollupWalletManager {
        with(input.wallet) {
            return EthereumOptimisticRollupWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = OptimismProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                ),
            )
        }
    }
}