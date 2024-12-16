package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.*
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object EthereumLikeWalletManagerAssembly : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            return EthereumWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = getProvidersBuilder(blockchain, input.providerTypes, input.config)
                        .build(blockchain),
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getProvidersBuilder(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): NetworkProvidersBuilder<EthereumJsonRpcProvider> {
        return when (blockchain) {
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> ArbitrumProvidersBuilder(providerTypes, config)
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> AvalancheProvidersBuilder(providerTypes, config)
            Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> EthereumClassicProvidersBuilder(
                providerTypes = providerTypes,
                config = config,
            )
            Blockchain.Fantom, Blockchain.FantomTestnet -> FantomProvidersBuilder(providerTypes, config)
            Blockchain.RSK -> RSKProvidersBuilder(providerTypes, config)
            Blockchain.BSC, Blockchain.BSCTestnet -> BSCProvidersBuilder(providerTypes, config)
            Blockchain.Polygon, Blockchain.PolygonTestnet -> PolygonProvidersBuilder(providerTypes, config)
            Blockchain.Gnosis -> GnosisProvidersBuilder(providerTypes, config)
            Blockchain.Dischain -> DischainProvidersBuilder(providerTypes)
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> EthereumPowProvidersBuilder(providerTypes, config)
            Blockchain.Kava, Blockchain.KavaTestnet -> KavaProvidersBuilder(providerTypes, config)
            Blockchain.Cronos -> CronosProvidersBuilder(providerTypes, config)
            Blockchain.OctaSpace -> OctaSpaceProvidersBuilder(providerTypes)
            Blockchain.Playa3ull -> Playa3ullProvidersBuilder(providerTypes)
            Blockchain.Shibarium, Blockchain.ShibariumTestnet -> ShibariumProvidersBuilder(providerTypes, config)
            Blockchain.Aurora, Blockchain.AuroraTestnet -> AuroraProvidersBuilder(providerTypes, config)
            Blockchain.Areon, Blockchain.AreonTestnet -> AreonProvidersBuilder(providerTypes)
            Blockchain.PulseChain, Blockchain.PulseChainTestnet -> PulseChainProvidersBuilder(providerTypes, config)
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> ZkSyncEraProvidersBuilder(providerTypes, config)
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet -> MoonbeamProvidersBuilder(providerTypes, config)
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet -> PolygonZkEVMProvidersBuilder(
                providerTypes = providerTypes,
                config = config,
            )
            Blockchain.Moonriver, Blockchain.MoonriverTestnet -> MoonriverProvidersBuilder(providerTypes)
            Blockchain.Flare, Blockchain.FlareTestnet -> FlareProvidersBuilder(providerTypes, config)
            Blockchain.Taraxa, Blockchain.TaraxaTestnet -> TaraxaProvidersBuilder(providerTypes)
            Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet -> EnergyWebChainProvidersBuilder(providerTypes)
            Blockchain.Core, Blockchain.CoreTestnet -> CoreProvidersBuilder(providerTypes)
            Blockchain.Chiliz, Blockchain.ChilizTestnet -> ChilizProvidersBuilder(providerTypes)
            Blockchain.Xodex -> XodexProvidersBuilder(providerTypes)
            Blockchain.Canxium -> CanxiumProvidersBuilder(providerTypes)
            else -> error("Unsupported blockchain: $blockchain")
        }
    }
}