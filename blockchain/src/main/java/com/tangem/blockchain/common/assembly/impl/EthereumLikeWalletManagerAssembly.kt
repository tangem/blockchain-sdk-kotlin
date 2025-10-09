package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumLikeJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.*
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.quai.QuaiJsonRpcProvider
import com.tangem.blockchain.blockchains.quai.QuaiNetworkService
import com.tangem.blockchain.blockchains.quai.QuaiWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.nft.NFTProviderFactory
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderFactory

internal class EthereumLikeWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
) : WalletManagerAssembly<EthereumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): EthereumWalletManager {
        with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                getProvidersBuilder(
                    blockchain = blockchain,
                    providerTypes = input.providerTypes,
                    config = input.config,
                ).build(blockchain),
            )
            val yieldLendingProvider = YieldSupplyProviderFactory(dataStorage).makeProvider(this, multiNetworkProvider)

            return createWalletManager(
                input = input,
                yieldLendingProvider = yieldLendingProvider,
                multiNetworkProvider = multiNetworkProvider,
            )
        }
    }

    private fun createNetworkService(
        blockchain: Blockchain,
        multiNetworkProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
        yieldSupplyProvider: YieldSupplyProvider,
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
                )
            }
        }
    }

    private fun createWalletManager(
        input: WalletManagerAssemblyInput,
        yieldLendingProvider: YieldSupplyProvider,
        multiNetworkProvider: MultiNetworkProvider<out EthereumLikeJsonRpcProvider>,
    ): EthereumWalletManager {
        with(input.wallet) {
            return when (blockchain) {
                Blockchain.Quai, Blockchain.QuaiTestnet -> {
                    QuaiWalletManager(
                        wallet = this,
                        transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                        networkProvider = createNetworkService(
                            blockchain = blockchain,
                            yieldSupplyProvider = yieldLendingProvider,
                            multiNetworkProvider = multiNetworkProvider,
                        ),
                        transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(
                            blockchain = blockchain,
                            config = input.config,
                        ),
                        nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                        yieldSupplyProvider = yieldLendingProvider,
                        supportsENS = false,
                    )
                }
                else -> {
                    EthereumWalletManager(
                        wallet = this,
                        transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                        networkProvider = createNetworkService(
                            blockchain = blockchain,
                            yieldSupplyProvider = yieldLendingProvider,
                            multiNetworkProvider = multiNetworkProvider,
                        ),
                        transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(
                            blockchain = blockchain,
                            config = input.config,
                        ),
                        nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
                        yieldSupplyProvider = yieldLendingProvider,
                        supportsENS = false,
                    )
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getProvidersBuilder(
        blockchain: Blockchain,
        providerTypes: List<ProviderType>,
        config: BlockchainSdkConfig,
    ): NetworkProvidersBuilder<EthereumLikeJsonRpcProvider> {
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
            Blockchain.Scroll, Blockchain.ScrollTestnet -> ScrollProvidersBuilder(providerTypes)
            Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet -> ZkLinkNovaProvidersBuilder(providerTypes)
            Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet -> HyperliquidProvidersBuilder(providerTypes)
            Blockchain.Quai, Blockchain.QuaiTestnet -> QuaiProvidersBuilder(providerTypes)
            Blockchain.Linea, Blockchain.LineaTestnet -> LineaProvidersBuilder(providerTypes, config)
            Blockchain.ArbitrumNova -> ArbitrumNovaProvidersBuilder(providerTypes, config)
            else -> error("Unsupported blockchain: $blockchain")
        }
    }
}