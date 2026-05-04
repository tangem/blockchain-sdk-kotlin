package com.tangem.blockchain.assetsdiscovery

import com.tangem.blockchain.assetsdiscovery.providers.bitcoin.BitcoinAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.radiant.RadiantAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.alephium.AlephiumAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.algorand.AlgorandAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.aptos.AptosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.binance.BinanceAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.cardano.CardanoAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.casper.CasperAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.chia.ChiaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.cosmos.CosmosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.filecoin.FilecoinAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.hedera.HederaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.kaspa.KaspaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.koinos.KoinosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.evm.MoralisEvmAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.near.NearAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.polkadot.PolkadotAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.solana.SolanaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.stellar.StellarAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.sui.SuiAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.tezos.TezosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.ton.TonAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.tron.TronAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.vechain.VeChainAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.xrp.XrpAssetsDiscoveryService
import com.tangem.blockchain.blockchains.algorand.AlgorandProvidersBuilder
import com.tangem.blockchain.blockchains.alephium.AlephiumProvidersBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinProvidersBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashProvidersBuilder
import com.tangem.blockchain.blockchains.clore.CloreProvidersBuilder
import com.tangem.blockchain.blockchains.dash.DashProvidersBuilder
import com.tangem.blockchain.blockchains.dogecoin.DogecoinProvidersBuilder
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.blockchains.factorn.Fact0rnProvidersBuilder
import com.tangem.blockchain.blockchains.factorn.network.Fact0rnNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinProvidersBuilder
import com.tangem.blockchain.blockchains.pepecoin.PepecoinProvidersBuilder
import com.tangem.blockchain.blockchains.pepecoin.network.PepecoinNetworkService
import com.tangem.blockchain.blockchains.radiant.RadiantProvidersBuilder
import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.blockchains.ravencoin.RavencoinProvidersBuilder
import com.tangem.blockchain.blockchains.alephium.network.AlephiumNetworkService
import com.tangem.blockchain.blockchains.aptos.AptosProvidersBuilder
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.blockchains.cardano.CardanoProvidersBuilder
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.casper.CasperProvidersBuilder
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkService
import com.tangem.blockchain.blockchains.chia.ChiaProvidersBuilder
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkService
import com.tangem.blockchain.blockchains.cosmos.CosmosProvidersBuilder
import com.tangem.blockchain.blockchains.filecoin.FilecoinProvidersBuilder
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkService
import com.tangem.blockchain.blockchains.hedera.HederaProvidersBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaProvidersBuilder
import com.tangem.blockchain.blockchains.koinos.KoinosProviderBuilder
import com.tangem.blockchain.blockchains.near.NearProvidersBuilder
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.polkadot.providers.*
import com.tangem.blockchain.blockchains.sei.SeiProvidersBuilder
import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.SolanaProvidersBuilder
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarProvidersBuilder
import com.tangem.blockchain.blockchains.sui.SuiNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.terra.TerraV1ProvidersBuilder
import com.tangem.blockchain.blockchains.terra.TerraV2ProvidersBuilder
import com.tangem.blockchain.blockchains.tezos.TezosProvidersBuilder
import com.tangem.blockchain.blockchains.ton.TonProvidersBuilder
import com.tangem.blockchain.blockchains.tron.TronProvidersBuilder
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.blockchains.vechain.VeChainProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.XRPProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider

class AssetsDiscoveryServiceFactory(
    private val config: BlockchainSdkConfig,
    private val providerTypes: Map<Blockchain, List<ProviderType>>,
) {

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun create(blockchain: Blockchain): AssetsDiscoveryService {
        val types = providerTypes[blockchain].orEmpty()
        return when (blockchain) {
            Blockchain.Solana, Blockchain.SolanaTestnet -> {
                val rpcClients = SolanaProvidersBuilder(types, config).build(blockchain)
                val networkServices = rpcClients.map { SolanaNetworkService(it) }
                SolanaAssetsDiscoveryService(MultiNetworkProvider(networkServices, blockchain), blockchain)
            }
            Blockchain.Tron, Blockchain.TronTestnet -> {
                val networkService = TronNetworkService(
                    rpcNetworkProviders = TronProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                TronAssetsDiscoveryService(networkService, blockchain)
            }
            Blockchain.XRP -> {
                val networkService = XrpNetworkService(
                    providers = XRPProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                XrpAssetsDiscoveryService(networkService, blockchain)
            }
            Blockchain.Stellar, Blockchain.StellarTestnet -> {
                val networkService = StellarNetworkService(
                    isTestnet = blockchain.isTestnet(),
                    providers = StellarProvidersBuilder(types, config).build(blockchain),
                )
                StellarAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Binance, Blockchain.BinanceTestnet -> {
                BinanceAssetsDiscoveryService(BinanceNetworkService(blockchain.isTestnet()), blockchain)
            }
            Blockchain.Cosmos, Blockchain.CosmosTestnet -> {
                val providers = CosmosProvidersBuilder(types, config).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.TerraV1 -> {
                val providers = TerraV1ProvidersBuilder(types, config).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.TerraV2 -> {
                val providers = TerraV2ProvidersBuilder(types, config).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Sei, Blockchain.SeiTestnet -> {
                val providers = SeiProvidersBuilder(types).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.TON, Blockchain.TONTestnet -> {
                val providers = TonProvidersBuilder(types, config).build(blockchain)
                TonAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Near, Blockchain.NearTestnet -> {
                val providers = NearProvidersBuilder(types, config).build(blockchain)
                NearAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Algorand, Blockchain.AlgorandTestnet -> {
                val providers = AlgorandProvidersBuilder(types, config).build(blockchain)
                AlgorandAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Polkadot, Blockchain.PolkadotTestnet,
            Blockchain.Kusama,
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet,
            Blockchain.Joystream,
            Blockchain.Bittensor,
            Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet,
            -> {
                val providers = buildPolkadotProviders(blockchain, types)
                val networkService = PolkadotNetworkService(providers = providers, blockchain = blockchain)
                PolkadotAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Aptos, Blockchain.AptosTestnet -> {
                val providers = AptosProvidersBuilder(providerTypes = types, config = config).build(blockchain)
                AptosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Sui, Blockchain.SuiTestnet -> {
                val providers = SuiNetworkProvidersBuilder(types, config).build(blockchain)
                SuiAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Hedera, Blockchain.HederaTestnet -> {
                val providers = HederaProvidersBuilder(types, config).build(blockchain)
                HederaAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.VeChain, Blockchain.VeChainTestnet -> {
                val providers = VeChainProvidersBuilder(types, config).build(blockchain)
                VeChainAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Kaspa, Blockchain.KaspaTestnet -> {
                val providers = KaspaProvidersBuilder(types, config).build(blockchain)
                KaspaAssetsDiscoveryService(
                    multiNetworkProvider = MultiNetworkProvider(providers, blockchain),
                    blockchain = blockchain,
                )
            }
            Blockchain.Cardano -> {
                val networkService = CardanoNetworkService(
                    providers = CardanoProvidersBuilder(providerTypes = types, config = config).build(blockchain),
                    blockchain = blockchain,
                )
                CardanoAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Tezos -> {
                val providers = TezosProvidersBuilder(types, config).build(blockchain)
                TezosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Chia, Blockchain.ChiaTestnet -> {
                val networkService = ChiaNetworkService(
                    chiaNetworkProviders = ChiaProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                ChiaAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Koinos, Blockchain.KoinosTestnet -> {
                val providers = KoinosProviderBuilder(types, config).build(blockchain)
                KoinosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Filecoin -> {
                val networkService = FilecoinNetworkService(
                    providers = FilecoinProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                FilecoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Casper, Blockchain.CasperTestnet -> {
                val networkService = CasperNetworkService(
                    providers = CasperProvidersBuilder(types, config, blockchain).build(blockchain),
                    blockchain = blockchain,
                )
                CasperAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Alephium, Blockchain.AlephiumTestnet -> {
                val networkService = AlephiumNetworkService(
                    providers = AlephiumProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                AlephiumAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                val providers = BitcoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                val providers = BitcoinCashProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Litecoin -> {
                val providers = LitecoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Dogecoin -> {
                val providers = DogecoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Dash -> {
                val providers = DashProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> {
                val providers = RavencoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Clore -> {
                val providers = CloreProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Ducatus -> {
                val networkService = DucatusNetworkService()
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Pepecoin, Blockchain.PepecoinTestnet -> {
                val electrumProviders = PepecoinProvidersBuilder(types).build(blockchain)
                val networkService = PepecoinNetworkService(blockchain, electrumProviders)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Fact0rn -> {
                val electrumProviders = Fact0rnProvidersBuilder(types).build(blockchain)
                val networkService = Fact0rnNetworkService(blockchain, electrumProviders)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Radiant -> {
                val electrumProviders = RadiantProvidersBuilder(types).build(blockchain)
                val networkService = RadiantNetworkService(electrumProviders)
                RadiantAssetsDiscoveryService(networkService, blockchain)
            }
            else -> createEvmProvider(blockchain)
        }
    }

    private fun createEvmProvider(blockchain: Blockchain): AssetsDiscoveryService {
        if (blockchain.isEvmSupportedNetwork()) {
            return MoralisEvmAssetsDiscoveryService(blockchain = blockchain, apiKey = config.moralisApiKey)
        }
        return DefaultAssetsDiscoveryService
    }

    private fun Blockchain.isEvmSupportedNetwork(): Boolean = when (this) {
        Blockchain.Ethereum, // supported testnet - Sepolia (11155111)
        Blockchain.Arbitrum,
        Blockchain.Avalanche,
        Blockchain.Fantom,
        Blockchain.BSC, Blockchain.BSCTestnet,
        Blockchain.Polygon, // supported testnet - Amoy (80002)
        Blockchain.Cronos,
        Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
        Blockchain.Moonriver,
        Blockchain.Chiliz, Blockchain.ChilizTestnet,
        Blockchain.Optimism,
        Blockchain.Base, Blockchain.BaseTestnet,
        Blockchain.Gnosis, // supported testnet - Chiado (10200)
        Blockchain.Linea, Blockchain.LineaTestnet,
        Blockchain.PulseChain,
        Blockchain.Monad,
        Blockchain.Mantle,
        -> true
        else -> false
    }

    private fun buildPolkadotProviders(
        blockchain: Blockchain,
        types: List<ProviderType>,
    ): List<PolkadotNetworkProvider> {
        return when (blockchain) {
            Blockchain.Polkadot, Blockchain.PolkadotTestnet -> PolkadotProvidersBuilder(types, config).build(blockchain)
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> AlephZeroProvidersBuilder(
                types,
                config,
            ).build(blockchain)
            Blockchain.Kusama -> KusamaProvidersBuilder(types, config).build(blockchain)
            Blockchain.Joystream -> JoyStreamProvidersBuilder(types).build(blockchain)
            Blockchain.Bittensor -> BittensorProvidersBuilder(types, config).build(blockchain)
            Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet -> EnergyWebXProvidersBuilder(types).build(blockchain)
            else -> emptyList()
        }
    }
}