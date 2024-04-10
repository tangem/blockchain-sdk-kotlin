package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider

internal object EthereumLikeWalletManagerAssembly : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = getProvidersBuilder(blockchain, input.providerTypes, input.config)
                        .build(blockchain),
                ),
                transactionHistoryProvider = blockchain.getTransactionHistoryProvider(input.config),
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getProvidersBuilder(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): EthereumLikeProvidersBuilder {
        return when (blockchain) {
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> ArbitrumProvidersBuilder(providerTypes, config)
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> AvalancheProvidersBuilder(providerTypes, config)
            Blockchain.EthereumTestnet, Blockchain.EthereumClassicTestnet -> EthereumClassicProvidersBuilder(
                providerTypes,
                config,
            )
            Blockchain.Fantom, Blockchain.FantomTestnet -> FantomProvidersBuilder(providerTypes, config)
            Blockchain.RSK -> RSKProvidersBuilder(providerTypes, config)
            Blockchain.BSC, Blockchain.BSCTestnet -> BSCProvidersBuilder(providerTypes, config)
            Blockchain.Polygon, Blockchain.PolygonTestnet -> PolygonProvidersBuilder(providerTypes, config)
            Blockchain.Gnosis -> GnosisProvidersBuilder(providerTypes, config)
            Blockchain.Dischain -> DischainProvidersBuilder(providerTypes, config)
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> EthereumPowProvidersBuilder(providerTypes, config)
            Blockchain.Kava, Blockchain.KavaTestnet -> KavaProvidersBuilder(providerTypes, config)
            Blockchain.Cronos -> CronosProvidersBuilder(providerTypes, config)
            Blockchain.OctaSpace -> OctaSpaceProvidersBuilder(providerTypes, config)
            Blockchain.Playa3ull -> Playa3ullProvidersBuilder(providerTypes, config)
            Blockchain.Shibarium, Blockchain.ShibariumTestnet -> ShibariumProvidersBuilder(providerTypes, config)
            Blockchain.Aurora, Blockchain.AuroraTestnet -> AuroraProvidersBuilder(providerTypes, config)
            Blockchain.Areon, Blockchain.AreonTestnet -> AreonProvidersBuilder(providerTypes, config)
            Blockchain.PulseChain, Blockchain.PulseChainTestnet -> PulseChainProvidersBuilder(providerTypes, config)
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> ZkSyncEraProvidersBuilder(providerTypes, config)
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet -> MoonbeamProvidersBuilder(providerTypes, config)
            Blockchain.Manta, Blockchain.MantaTestnet -> MantaProvidersBuilder(providerTypes, config)
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet -> PolygonZkEVMProvidersBuilder(
                providerTypes,
                config,
            )
            Blockchain.Base, Blockchain.BaseTestnet -> BaseProvidersBuilder(providerTypes, config)
            Blockchain.Moonriver, Blockchain.MoonriverTestnet -> MoonriverProvidersBuilder(providerTypes, config)
            Blockchain.Mantle, Blockchain.MantleTestnet -> MantleProvidersBuilder(providerTypes, config)
            Blockchain.Flare, Blockchain.FlareTestnet -> FlareProvidersBuilder(providerTypes, config)
            Blockchain.Taraxa, Blockchain.TaraxaTestnet -> TaraxaProvidersBuilder(providerTypes, config)
            else -> error("Unsupported blockchain: $blockchain")
        }
    }
}