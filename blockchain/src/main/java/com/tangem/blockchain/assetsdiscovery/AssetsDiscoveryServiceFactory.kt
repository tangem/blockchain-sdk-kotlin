package com.tangem.blockchain.assetsdiscovery

import com.tangem.blockchain.assetsdiscovery.providers.alephium.AlephiumAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.algorand.AlgorandAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.aptos.AptosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.binance.BinanceAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.bitcoin.BitcoinAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.cardano.CardanoAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.casper.CasperAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.chia.ChiaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.cosmos.CosmosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.evm.DefaultEvmAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.evm.MoralisEvmAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.filecoin.FilecoinAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.icp.ICPAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.kaspa.KaspaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.koinos.KoinosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.near.NearAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.nexa.NexaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.polkadot.PolkadotAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.radiant.RadiantAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.solana.SolanaAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.stellar.StellarAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.sui.SuiAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.tezos.TezosAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.ton.TonAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.tron.TronAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.vechain.VeChainAssetsDiscoveryService
import com.tangem.blockchain.assetsdiscovery.providers.xrp.XrpAssetsDiscoveryService
import com.tangem.blockchain.blockchains.alephium.AlephiumProvidersBuilder
import com.tangem.blockchain.blockchains.alephium.network.AlephiumNetworkService
import com.tangem.blockchain.blockchains.algorand.AlgorandProvidersBuilder
import com.tangem.blockchain.blockchains.aptos.AptosProvidersBuilder
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinProvidersBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashProvidersBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoProvidersBuilder
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.casper.CasperProvidersBuilder
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkService
import com.tangem.blockchain.blockchains.chia.ChiaProvidersBuilder
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkService
import com.tangem.blockchain.blockchains.clore.CloreProvidersBuilder
import com.tangem.blockchain.blockchains.cosmos.CosmosProvidersBuilder
import com.tangem.blockchain.blockchains.dash.DashProvidersBuilder
import com.tangem.blockchain.blockchains.decimal.DecimalProvidersBuilder
import com.tangem.blockchain.blockchains.dogecoin.DogecoinProvidersBuilder
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.blockchains.ethereum.network.EthereumLikeJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.providers.ApeChainProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.AreonProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.AuroraProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.ArbitrumNovaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.BitrockProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.BlastProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CanxiumProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CoreProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.CyberProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.DischainProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.EnergyWebChainProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.EthereumClassicProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.EthereumPowProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.FlareProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.HyperliquidProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.KavaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.MantaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.MantleProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.OctaSpaceProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.OdysseyChainProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.Playa3ullProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.PolygonZkEVMProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.QuaiProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.RSKProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.ScrollProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.ShibariumProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.SonicProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.TaraxaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.VanarChainProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.XodexProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.ZkLinkNovaProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.providers.ZkSyncEraProvidersBuilder
import com.tangem.blockchain.blockchains.factorn.Fact0rnProvidersBuilder
import com.tangem.blockchain.blockchains.factorn.network.Fact0rnNetworkService
import com.tangem.blockchain.blockchains.filecoin.FilecoinProvidersBuilder
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkService
import com.tangem.blockchain.blockchains.icp.ICPProvidersBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaProvidersBuilder
import com.tangem.blockchain.blockchains.koinos.KoinosProviderBuilder
import com.tangem.blockchain.blockchains.litecoin.LitecoinProvidersBuilder
import com.tangem.blockchain.blockchains.near.NearProvidersBuilder
import com.tangem.blockchain.blockchains.nexa.NexaProvidersBuilder
import com.tangem.blockchain.blockchains.pepecoin.PepecoinProvidersBuilder
import com.tangem.blockchain.blockchains.pepecoin.network.PepecoinNetworkService
import com.tangem.blockchain.blockchains.plasma.PlasmaProvidersBuilder
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.polkadot.providers.*
import com.tangem.blockchain.blockchains.radiant.RadiantProvidersBuilder
import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.blockchains.ravencoin.RavencoinProvidersBuilder
import com.tangem.blockchain.blockchains.sei.SeiProvidersBuilder
import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.SolanaProvidersBuilder
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarProvidersBuilder
import com.tangem.blockchain.blockchains.sui.SuiNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.telos.TelosProvidersBuilder
import com.tangem.blockchain.blockchains.terra.TerraV1ProvidersBuilder
import com.tangem.blockchain.blockchains.terra.TerraV2ProvidersBuilder
import com.tangem.blockchain.blockchains.tezos.TezosProvidersBuilder
import com.tangem.blockchain.blockchains.ton.TonProvidersBuilder
import com.tangem.blockchain.blockchains.tron.TronProvidersBuilder
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.blockchains.vechain.VeChainProvidersBuilder
import com.tangem.blockchain.blockchains.xdc.XDCProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.XRPProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider

/**
 * Factory that maps each supported [Blockchain] to the appropriate [AssetsDiscoveryService].
 *
 * Per-blockchain support matrix (sorted alphabetically):
 *
 * | Blockchain       | Supported assets |
 * |------------------|------------------|
 * | Alephium         | coins            |
 * | AlephZero        | coins            |
 * | Algorand         | coins            |
 * | ApeChain         | coins            |
 * | Aptos            | coins + tokens   |
 * | Arbitrum         | coins + tokens   |
 * | ArbitrumNova     | coins            |
 * | Areon            | coins            |
 * | Aurora           | coins            |
 * | Avalanche        | coins + tokens   |
 * | Base             | coins + tokens   |
 * | Binance          | coins + tokens   |
 * | Bitcoin          | coins            |
 * | BitcoinCash      | coins            |
 * | Bitrock          | coins            |
 * | Bittensor        | coins            |
 * | Blast            | coins            |
 * | BSC              | coins + tokens   |
 * | Canxium          | coins            |
 * | Cardano          | coins + tokens   |
 * | Casper           | coins            |
 * | Chia             | coins            |
 * | Chiliz           | coins + tokens   |
 * | Clore            | coins            |
 * | Core             | coins            |
 * | Cosmos           | coins + tokens   |
 * | Cronos           | coins + tokens   |
 * | Cyber            | coins            |
 * | Dash             | coins            |
 * | Decimal          | coins            |
 * | Dischain         | coins            |
 * | Dogecoin         | coins            |
 * | Ducatus          | coins            |
 * | EnergyWebChain   | coins            |
 * | EnergyWebX       | coins            |
 * | Ethereum         | coins + tokens   |
 * | EthereumClassic  | coins            |
 * | EthereumPow      | coins            |
 * | Fact0rn          | coins            |
 * | Fantom           | coins + tokens   |
 * | Filecoin         | coins            |
 * | Flare            | coins            |
 * | Gnosis           | coins + tokens   |
 * | Hyperliquid      | coins            |
 * | InternetComputer | coins            |
 * | Joystream        | coins            |
 * | Kaspa            | coins            |
 * | Kava             | coins            |
 * | Koinos           | coins            |
 * | Kusama           | coins            |
 * | Linea            | coins + tokens   |
 * | Litecoin         | coins            |
 * | Manta            | coins            |
 * | Mantle           | coins            |
 * | Monad            | coins + tokens   |
 * | Moonbeam         | coins + tokens   |
 * | Moonriver        | coins + tokens   |
 * | Near             | coins            |
 * | Nexa             | coins            |
 * | OctaSpace        | coins            |
 * | OdysseyChain     | coins            |
 * | Optimism         | coins + tokens   |
 * | Pepecoin         | coins            |
 * | Plasma           | coins            |
 * | Playa3ull        | coins            |
 * | Polkadot         | coins            |
 * | Polygon          | coins + tokens   |
 * | PolygonZkEVM     | coins            |
 * | PulseChain       | coins + tokens   |
 * | Quai             | coins            |
 * | Radiant          | coins            |
 * | Ravencoin        | coins            |
 * | RSK              | coins            |
 * | Scroll           | coins            |
 * | Sei              | coins + tokens   |
 * | Shibarium        | coins            |
 * | Solana           | coins + tokens   |
 * | Sonic            | coins            |
 * | Stellar          | coins + tokens   |
 * | Sui              | coins + tokens   |
 * | Taraxa           | coins            |
 * | Telos            | coins            |
 * | TerraV1          | coins + tokens   |
 * | TerraV2          | coins + tokens   |
 * | Tezos            | coins            |
 * | TON              | coins            |
 * | Tron             | coins + tokens   |
 * | VanarChain       | coins            |
 * | VeChain          | coins            |
 * | XDC              | coins            |
 * | Xodex            | coins            |
 * | XRP              | coins + tokens   |
 * | ZkLinkNova       | coins            |
 * | ZkSyncEra        | coins            |
 *
 * Unsupported (fallback to [DefaultAssetsDiscoveryService]): Hedera.
 */
@Suppress("LargeClass")
class AssetsDiscoveryServiceFactory(
    private val config: BlockchainSdkConfig,
    private val providerTypes: Map<Blockchain, List<ProviderType>>,
) {

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun create(blockchain: Blockchain): AssetsDiscoveryService {
        val types = providerTypes[blockchain].orEmpty()
        return when (blockchain) {
            Blockchain.Alephium, Blockchain.AlephiumTestnet -> {
                val networkService = AlephiumNetworkService(
                    providers = AlephiumProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                AlephiumAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> createPolkadotDiscoveryService(blockchain, types)
            Blockchain.Algorand, Blockchain.AlgorandTestnet -> {
                val providers = AlgorandProvidersBuilder(types, config).build(blockchain)
                AlgorandAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.ApeChain, Blockchain.ApeChainTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = ApeChainProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Aptos, Blockchain.AptosTestnet -> {
                val providers = AptosProvidersBuilder(providerTypes = types, config = config).build(blockchain)
                AptosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.ArbitrumNova -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = ArbitrumNovaProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Areon, Blockchain.AreonTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = AreonProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Aurora, Blockchain.AuroraTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = AuroraProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Base, Blockchain.BaseTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Binance, Blockchain.BinanceTestnet -> {
                BinanceAssetsDiscoveryService(BinanceNetworkService(blockchain.isTestnet()), blockchain)
            }
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                val providers = BitcoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                val providers = BitcoinCashProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Bitrock, Blockchain.BitrockTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = BitrockProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Bittensor -> createPolkadotDiscoveryService(blockchain, types)
            Blockchain.Blast, Blockchain.BlastTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = BlastProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.BSC, Blockchain.BSCTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Canxium -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = CanxiumProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Cardano -> {
                val networkService = CardanoNetworkService(
                    providers = CardanoProvidersBuilder(providerTypes = types, config = config).build(blockchain),
                    blockchain = blockchain,
                )
                CardanoAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Casper, Blockchain.CasperTestnet -> {
                val networkService = CasperNetworkService(
                    providers = CasperProvidersBuilder(types, config, blockchain).build(blockchain),
                    blockchain = blockchain,
                )
                CasperAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Chia, Blockchain.ChiaTestnet -> {
                val networkService = ChiaNetworkService(
                    chiaNetworkProviders = ChiaProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                ChiaAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Chiliz, Blockchain.ChilizTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Clore -> {
                val providers = CloreProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Core, Blockchain.CoreTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = CoreProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Cosmos, Blockchain.CosmosTestnet -> {
                val providers = CosmosProvidersBuilder(types, config).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Cronos -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Cyber, Blockchain.CyberTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = CyberProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Dash -> {
                val providers = DashProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Decimal, Blockchain.DecimalTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = DecimalProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Dischain -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = DischainProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Dogecoin -> {
                val providers = DogecoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Ducatus -> {
                val networkService = DucatusNetworkService()
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = EnergyWebChainProvidersBuilder(types).build(blockchain),
            )
            Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet -> createPolkadotDiscoveryService(blockchain, types)
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = EthereumClassicProvidersBuilder(providerTypes = types, config = config).build(blockchain),
            )
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = EthereumPowProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Fact0rn -> {
                val electrumProviders = Fact0rnProvidersBuilder(types).build(blockchain)
                val networkService = Fact0rnNetworkService(blockchain, electrumProviders)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Fantom, Blockchain.FantomTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Filecoin -> {
                val networkService = FilecoinNetworkService(
                    providers = FilecoinProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                FilecoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Flare, Blockchain.FlareTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = FlareProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Gnosis -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = HyperliquidProvidersBuilder(types).build(blockchain),
            )
            Blockchain.InternetComputer -> {
                // The public key is only used for signing, which is not exercised by the discovery path.
                val providers = ICPProvidersBuilder(
                    providerTypes = types,
                    walletPublicKey = Wallet.PublicKey(seedKey = ByteArray(size = 0), derivationType = null),
                ).build(blockchain)
                if (providers.isEmpty()) {
                    DefaultAssetsDiscoveryService
                } else {
                    ICPAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
                }
            }
            Blockchain.Joystream -> createPolkadotDiscoveryService(blockchain, types)
            Blockchain.Kaspa, Blockchain.KaspaTestnet -> {
                val providers = KaspaProvidersBuilder(types, config).build(blockchain)
                KaspaAssetsDiscoveryService(
                    multiNetworkProvider = MultiNetworkProvider(providers, blockchain),
                    blockchain = blockchain,
                )
            }
            Blockchain.Kava, Blockchain.KavaTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = KavaProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Koinos, Blockchain.KoinosTestnet -> {
                val providers = KoinosProviderBuilder(types, config).build(blockchain)
                KoinosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Kusama -> createPolkadotDiscoveryService(blockchain, types)
            Blockchain.Linea, Blockchain.LineaTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Litecoin -> {
                val providers = LitecoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Manta, Blockchain.MantaTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = MantaProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Mantle, Blockchain.MantleTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = MantleProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Monad, Blockchain.MonadTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Moonriver -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Near, Blockchain.NearTestnet -> {
                val providers = NearProvidersBuilder(types, config).build(blockchain)
                NearAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Nexa, Blockchain.NexaTestnet -> {
                val providers = NexaProvidersBuilder(types).build(blockchain)
                if (providers.isEmpty()) {
                    DefaultAssetsDiscoveryService
                } else {
                    NexaAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
                }
            }
            Blockchain.OctaSpace -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = OctaSpaceProvidersBuilder(types).build(blockchain),
            )
            Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = OdysseyChainProvidersBuilder(providerTypes = types, config = config).build(blockchain),
            )
            Blockchain.Optimism, Blockchain.OptimismTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Pepecoin, Blockchain.PepecoinTestnet -> {
                val electrumProviders = PepecoinProvidersBuilder(types).build(blockchain)
                val networkService = PepecoinNetworkService(blockchain, electrumProviders)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Plasma, Blockchain.PlasmaTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = PlasmaProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Playa3ull -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = Playa3ullProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Polkadot, Blockchain.PolkadotTestnet -> createPolkadotDiscoveryService(blockchain, types)
            Blockchain.Polygon, Blockchain.PolygonTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = PolygonZkEVMProvidersBuilder(providerTypes = types, config = config).build(blockchain),
            )
            Blockchain.PulseChain, Blockchain.PulseChainTestnet -> createMoralisEvmDiscoveryService(blockchain)
            Blockchain.Quai, Blockchain.QuaiTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = QuaiProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Radiant -> {
                val electrumProviders = RadiantProvidersBuilder(types).build(blockchain)
                val networkService = RadiantNetworkService(electrumProviders)
                RadiantAssetsDiscoveryService(networkService, blockchain)
            }
            Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> {
                val providers = RavencoinProvidersBuilder(types, config).build(blockchain)
                BitcoinAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.RSK -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = RSKProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Scroll, Blockchain.ScrollTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = ScrollProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Sei, Blockchain.SeiTestnet -> {
                val providers = SeiProvidersBuilder(types).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Shibarium, Blockchain.ShibariumTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = ShibariumProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Solana, Blockchain.SolanaTestnet -> {
                val rpcClients = SolanaProvidersBuilder(types, config).build(blockchain)
                val networkServices = rpcClients.map { SolanaNetworkService(it) }
                SolanaAssetsDiscoveryService(MultiNetworkProvider(networkServices, blockchain), blockchain)
            }
            Blockchain.Sonic, Blockchain.SonicTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = SonicProvidersBuilder(providerTypes = types, config = config).build(blockchain),
            )
            Blockchain.Stellar, Blockchain.StellarTestnet -> {
                val networkService = StellarNetworkService(
                    isTestnet = blockchain.isTestnet(),
                    providers = StellarProvidersBuilder(types, config).build(blockchain),
                )
                StellarAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
            }
            Blockchain.Sui, Blockchain.SuiTestnet -> {
                val providers = SuiNetworkProvidersBuilder(types, config).build(blockchain)
                SuiAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Taraxa, Blockchain.TaraxaTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = TaraxaProvidersBuilder(types).build(blockchain),
            )
            Blockchain.Telos, Blockchain.TelosTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = TelosProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.TerraV1 -> {
                val providers = TerraV1ProvidersBuilder(types, config).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.TerraV2 -> {
                val providers = TerraV2ProvidersBuilder(types, config).build(blockchain)
                CosmosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Tezos -> {
                val providers = TezosProvidersBuilder(types, config).build(blockchain)
                TezosAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.TON, Blockchain.TONTestnet -> {
                val providers = TonProvidersBuilder(types, config).build(blockchain)
                TonAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.Tron, Blockchain.TronTestnet -> {
                val networkService = TronNetworkService(
                    rpcNetworkProviders = TronProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                TronAssetsDiscoveryService(networkService, blockchain)
            }
            Blockchain.VanarChain, Blockchain.VanarChainTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = VanarChainProvidersBuilder(types).build(blockchain),
            )
            Blockchain.VeChain, Blockchain.VeChainTestnet -> {
                val providers = VeChainProvidersBuilder(types, config).build(blockchain)
                VeChainAssetsDiscoveryService(MultiNetworkProvider(providers, blockchain), blockchain)
            }
            Blockchain.XDC, Blockchain.XDCTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = XDCProvidersBuilder(types, config).build(blockchain),
            )
            Blockchain.Xodex -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = XodexProvidersBuilder(types).build(blockchain),
            )
            Blockchain.XRP -> {
                val networkService = XrpNetworkService(
                    providers = XRPProvidersBuilder(types, config).build(blockchain),
                    blockchain = blockchain,
                )
                XrpAssetsDiscoveryService(networkService, blockchain)
            }
            Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = ZkLinkNovaProvidersBuilder(types).build(blockchain),
            )
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> createDefaultEvmDiscoveryService(
                blockchain = blockchain,
                providers = ZkSyncEraProvidersBuilder(types, config).build(blockchain),
            )
            else -> DefaultAssetsDiscoveryService
        }
    }

    private fun createMoralisEvmDiscoveryService(blockchain: Blockchain): AssetsDiscoveryService {
        return MoralisEvmAssetsDiscoveryService(blockchain = blockchain, apiKey = config.moralisApiKey)
    }

    private fun <P : EthereumLikeJsonRpcProvider> createDefaultEvmDiscoveryService(
        blockchain: Blockchain,
        providers: List<P>,
    ): AssetsDiscoveryService {
        if (providers.isEmpty()) return DefaultAssetsDiscoveryService
        return DefaultEvmAssetsDiscoveryService(
            multiNetworkProvider = MultiNetworkProvider(providers, blockchain),
            blockchain = blockchain,
        )
    }

    private fun createPolkadotDiscoveryService(
        blockchain: Blockchain,
        types: List<ProviderType>,
    ): AssetsDiscoveryService {
        val providers = buildPolkadotProviders(blockchain, types)
        if (providers.isEmpty()) return DefaultAssetsDiscoveryService
        val networkService = PolkadotNetworkService(providers = providers, blockchain = blockchain)
        return PolkadotAssetsDiscoveryService(MultiNetworkProvider(listOf(networkService), blockchain), blockchain)
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