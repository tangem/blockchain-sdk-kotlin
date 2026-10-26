package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.blockchains.adi.AdiProvidersBuilder
import com.tangem.blockchain.blockchains.arc.ArcProvidersBuilder
import com.tangem.blockchain.blockchains.electroneum.ElectroneumProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.*
import com.tangem.blockchain.blockchains.igra.IgraProvidersBuilder
import com.tangem.blockchain.blockchains.onyx.OnyxProvidersBuilder
import com.tangem.blockchain.blockchains.optimism.OptimismProvidersBuilder
import com.tangem.blockchain.blockchains.plasma.PlasmaProvidersBuilder
import com.tangem.blockchain.blockchains.publicmasterpiece.PublicMasterpieceProvidersBuilder
import com.tangem.blockchain.blockchains.quai.QuaiJsonRpcProvider
import com.tangem.blockchain.blockchains.quai.QuaiNetworkService
import com.tangem.blockchain.blockchains.robinhood.RobinhoodProvidersBuilder
import com.tangem.blockchain.blockchains.seievm.SeiEvmProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider

/**
 * The network side of every EVM manager, assembled without a wallet: the RPC providers of a chain and the network
 * service on top of them. The wallet-scoped pieces (yield supply, pending transactions) are passed in by the caller
 * that has a wallet; a caller without one keeps the defaults.
 */
internal object EvmNetworkServiceFactory {

    fun createMultiProvider(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): MultiNetworkProvider<out EthereumLikeJsonRpcProvider> {
        return MultiNetworkProvider(
            providers = createProvidersBuilder(blockchain, providerTypes, config).build(blockchain),
            blockchain = blockchain,
        )
    }

    /**
     * Same as [createMultiProvider], but `null` when the chain is not assembled here or [providerTypes] yield no
     * RPC provider — for callers that have no wallet manager to fall back on.
     */
    fun createMultiProviderOrNull(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): MultiNetworkProvider<out EthereumLikeJsonRpcProvider>? {
        val builder = createProvidersBuilderOrNull(blockchain, providerTypes, config) ?: return null
        val providers = builder.build(blockchain)

        return if (providers.isEmpty()) null else MultiNetworkProvider(providers = providers, blockchain = blockchain)
    }

    fun createNetworkService(
        blockchain: Blockchain,
        multiNetworkProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
        yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
        blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    ): EthereumNetworkProvider {
        return when (blockchain) {
            Blockchain.Quai, Blockchain.QuaiTestnet -> {
                @Suppress("UNCHECKED_CAST")
                QuaiNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider as MultiNetworkProvider<QuaiJsonRpcProvider>,
                    yieldSupplyProvider = yieldSupplyProvider,
                )
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider as MultiNetworkProvider<EthereumJsonRpcProvider>,
                    yieldSupplyProvider = yieldSupplyProvider,
                    blockcypherNetworkProvider = blockcypherNetworkProvider,
                )
            }
        }
    }

    fun createNetworkServiceOrNull(
        blockchain: Blockchain,
        multiNetworkProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
        yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
        blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    ): EthereumNetworkProvider? {
        return when {
            blockchain == Blockchain.Quai || blockchain == Blockchain.QuaiTestnet -> {
                if (multiNetworkProvider.currentProvider !is QuaiJsonRpcProvider) return null

                @Suppress("UNCHECKED_CAST")
                QuaiNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider as MultiNetworkProvider<QuaiJsonRpcProvider>,
                    yieldSupplyProvider = yieldSupplyProvider,
                )
            }
            multiNetworkProvider.currentProvider is EthereumJsonRpcProvider -> {
                @Suppress("UNCHECKED_CAST")
                EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider as MultiNetworkProvider<EthereumJsonRpcProvider>,
                    yieldSupplyProvider = yieldSupplyProvider,
                    blockcypherNetworkProvider = blockcypherNetworkProvider,
                )
            }
            else -> null
        }
    }

    private fun createProvidersBuilder(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): NetworkProvidersBuilder<EthereumLikeJsonRpcProvider> {
        return createProvidersBuilderOrNull(blockchain, providerTypes, config)
            ?: error("Unsupported blockchain: $blockchain")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun createProvidersBuilderOrNull(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): NetworkProvidersBuilder<EthereumLikeJsonRpcProvider>? {
        return when (blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> EthereumProvidersBuilder(providerTypes, config)
            Blockchain.Optimism, Blockchain.OptimismTestnet -> OptimismProvidersBuilder(providerTypes, config)
            Blockchain.Base, Blockchain.BaseTestnet -> BaseProvidersBuilder(providerTypes, config)
            Blockchain.Manta, Blockchain.MantaTestnet -> MantaProvidersBuilder(providerTypes)
            Blockchain.Blast, Blockchain.BlastTestnet -> BlastProvidersBuilder(providerTypes, config)
            Blockchain.Cyber, Blockchain.CyberTestnet -> CyberProvidersBuilder(providerTypes)
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
            Blockchain.Monad, Blockchain.MonadTestnet -> MonadProvidersBuilder(providerTypes, config)
            Blockchain.Flare, Blockchain.FlareTestnet -> FlareProvidersBuilder(providerTypes, config)
            Blockchain.Taraxa, Blockchain.TaraxaTestnet -> TaraxaProvidersBuilder(providerTypes)
            Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet -> EnergyWebChainProvidersBuilder(providerTypes)
            Blockchain.Core, Blockchain.CoreTestnet -> CoreProvidersBuilder(providerTypes)
            Blockchain.Chiliz, Blockchain.ChilizTestnet -> ChilizProvidersBuilder(providerTypes)
            Blockchain.VanarChain, Blockchain.VanarChainTestnet -> VanarChainProvidersBuilder(providerTypes)
            Blockchain.Xodex -> XodexProvidersBuilder(providerTypes)
            Blockchain.Canxium -> CanxiumProvidersBuilder(providerTypes)
            Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet -> OdysseyChainProvidersBuilder(
                providerTypes = providerTypes,
                config = config,
            )
            Blockchain.Bitrock, Blockchain.BitrockTestnet -> BitrockProvidersBuilder(providerTypes)
            Blockchain.Sonic, Blockchain.SonicTestnet -> SonicProvidersBuilder(
                providerTypes = providerTypes,
                config = config,
            )
            Blockchain.ApeChain, Blockchain.ApeChainTestnet -> ApeChainProvidersBuilder(providerTypes)
            Blockchain.Scroll, Blockchain.ScrollTestnet -> ScrollProvidersBuilder(providerTypes, config)
            Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet -> ZkLinkNovaProvidersBuilder(providerTypes)
            Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet -> HyperliquidProvidersBuilder(providerTypes)
            Blockchain.Quai, Blockchain.QuaiTestnet -> QuaiProvidersBuilder(providerTypes)
            Blockchain.Linea, Blockchain.LineaTestnet -> LineaProvidersBuilder(providerTypes, config)
            Blockchain.ArbitrumNova -> ArbitrumNovaProvidersBuilder(providerTypes, config)
            Blockchain.Plasma, Blockchain.PlasmaTestnet -> PlasmaProvidersBuilder(providerTypes, config)
            Blockchain.Adi, Blockchain.AdiTestnet -> AdiProvidersBuilder(providerTypes, config)
            Blockchain.Arc, Blockchain.ArcTestnet -> ArcProvidersBuilder(providerTypes, config)
            Blockchain.Onyx, Blockchain.OnyxTestnet -> OnyxProvidersBuilder(providerTypes, config)
            Blockchain.SeiEvm, Blockchain.SeiEvmTestnet -> SeiEvmProvidersBuilder(providerTypes, config)
            Blockchain.Robinhood, Blockchain.RobinhoodTestnet -> RobinhoodProvidersBuilder(providerTypes, config)
            Blockchain.Igra, Blockchain.IgraTestnet -> IgraProvidersBuilder(providerTypes, config)
            Blockchain.Electroneum, Blockchain.ElectroneumTestnet -> ElectroneumProvidersBuilder(providerTypes, config)
            Blockchain.PublicMasterpiece -> PublicMasterpieceProvidersBuilder(providerTypes, config)
            else -> null
        }
    }
}