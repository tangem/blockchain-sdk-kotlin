package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.BaseProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.BlastProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CyberProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.MantaProvidersBuilder
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

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
                    jsonRpcProviders = getProvidersBuilder(blockchain, input.providerTypes, input.config)
                        .build(blockchain),
                ),
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
            else -> error("Unsupported blockchain: $blockchain")
        }
    }
}