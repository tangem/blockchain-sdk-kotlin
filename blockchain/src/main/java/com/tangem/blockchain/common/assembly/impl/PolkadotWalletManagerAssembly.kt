package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.polkadot.providers.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal object PolkadotWalletManagerAssembly : WalletManagerAssembly<PolkadotWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): PolkadotWalletManager {
        return with(input.wallet) {
            PolkadotWalletManager(
                wallet = this,
                networkProvider = PolkadotNetworkService(
                    providers = getNetworkProvidersBuilder(
                        providerTypes = input.providerTypes,
                        config = input.config,
                        blockchain = blockchain,
                    ).build(blockchain),
                ),
            )
        }
    }

    private fun getNetworkProvidersBuilder(
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
        blockchain: Blockchain,
    ): NetworkProvidersBuilder<PolkadotNetworkProvider> {
        return when (blockchain) {
            Blockchain.Polkadot, Blockchain.PolkadotTestnet -> PolkadotProvidersBuilder(providerTypes, config)
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> AlephZeroProvidersBuilder(providerTypes)
            Blockchain.Kusama -> KusamaProvidersBuilder(providerTypes, config)
            Blockchain.Joystream -> JoyStreamProvidersBuilder(providerTypes)
            Blockchain.Bittensor -> BittensorProvidersBuilder(providerTypes, config)
            Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet -> EnergyWebXProvidersBuilder(providerTypes)
            else -> error("$blockchain isn't supported")
        }
    }
}