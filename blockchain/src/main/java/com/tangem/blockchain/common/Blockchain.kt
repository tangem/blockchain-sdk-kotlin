package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.alephium.AlephiumAddressService
import com.tangem.blockchain.blockchains.aptos.AptosAddressService
import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressServiceFacade
import com.tangem.blockchain.blockchains.casper.CasperAddressService
import com.tangem.blockchain.blockchains.chia.ChiaAddressService
import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumDerivationData
import com.tangem.blockchain.blockchains.factorn.Fact0rnAddressService
import com.tangem.blockchain.blockchains.hedera.HederaAddressService
import com.tangem.blockchain.blockchains.kaspa.KaspaAddressService
import com.tangem.blockchain.blockchains.koinos.KoinosAddressService
import com.tangem.blockchain.blockchains.nexa.NexaAddressService
import com.tangem.blockchain.blockchains.pepecoin.PepecoinAddressService
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.blockchains.quai.QuaiAddressService
import com.tangem.blockchain.blockchains.radiant.RadiantAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.sui.SuiAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.blockchains.vechain.VeChainWalletManager
import com.tangem.blockchain.blockchains.xdc.XDCAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.*
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.ExternalLinkProviderFactory
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

/**
 * @param id sdk blockchain id
 * @param currency currency symbol
 * @param fullName network name
 */
@Suppress("LargeClass", "TooManyFunctions")
enum class Blockchain(
    val id: String,
    val currency: String,
    val fullName: String,
) {
    Unknown("", "", ""),
    Arbitrum("ARBITRUM-ONE", "ETH", "Arbitrum One"),
    ArbitrumTestnet("ARBITRUM/test", "ETH", "Arbitrum Testnet"),
    Avalanche("AVALANCHE", "AVAX", "Avalanche C-Chain"),
    AvalancheTestnet("AVALANCHE/test", "AVAX", "Avalanche C-Chain Testnet"),
    Binance("BINANCE", "BNB", "BNB Beacon Chain"),
    BinanceTestnet("BINANCE/test", "BNB", "BNB Beacon Chain Testnet"),
    BSC("BSC", "BNB", "BNB Smart Chain"),
    BSCTestnet("BSC/test", "BNB", "BNB Smart Chain Testnet"),
    Bitcoin("BTC", "BTC", "Bitcoin"),
    BitcoinTestnet("BTC/test", "BTC", "Bitcoin Testnet"),
    BitcoinCash("BCH", "BCH", "Bitcoin Cash"),
    BitcoinCashTestnet("BCH/test", "BCH", "Bitcoin Cash Testnet"),
    Cardano("CARDANO-S", "ADA", "Cardano"),
    Cosmos("cosmos", "ATOM", "Cosmos"),
    CosmosTestnet("cosmos/test", "ATOM", "Cosmos Testnet"),
    Dogecoin("DOGE", "DOGE", "Dogecoin"),
    Ducatus("DUC", "DUC", "Ducatus"),
    Ethereum("ETH", "ETH", "Ethereum"),
    EthereumTestnet("ETH/test", "ETH", "Ethereum Testnet"),
    EthereumClassic("ETC", "ETC", "Ethereum Classic"),
    EthereumClassicTestnet("ETC/test", "ETC", "Ethereum Classic Testnet"),
    Fantom("FTM", "FTM", "Fantom Opera"),
    FantomTestnet("FTM/test", "FTM", "Fantom Testnet"),
    Litecoin("LTC", "LTC", "Litecoin"),
    Near("NEAR", "NEAR", "NEAR Protocol"),
    NearTestnet("NEAR/test", "NEAR", "NEAR Protocol Testnet"),
    Polkadot("Polkadot", "DOT", "Polkadot"),
    PolkadotTestnet("Polkadot", "WND", "Polkadot Westend Testnet"),
    Kava("KAVA", "KAVA", "Kava EVM"),
    KavaTestnet("KAVA/test", "KAVA", "Kava EVM Testnet"),
    Kusama("Kusama", "KSM", "Kusama"),
    Polygon("POLYGON", "POL", "Polygon"),
    PolygonTestnet("POLYGON/test", "POL", "Polygon Testnet"),
    RSK("RSK", "RBTC", "RSK"),
    Sei("sei", "SEI", "Sei"),
    SeiTestnet("sei/test", "SEI", "Sei Testnet"),
    Stellar("XLM", "XLM", "Stellar"),
    StellarTestnet("XLM/test", "XLM", "Stellar Testnet"),
    Solana("SOLANA", "SOL", "Solana"),
    SolanaTestnet("SOLANA/test", "SOL", "Solana Testnet"),
    Tezos("XTZ", "XTZ", "Tezos"),
    Tron("TRON", "TRX", "Tron"),
    TronTestnet("TRON/test", "TRX", "Tron Testnet"),
    XRP("XRP", "XRP", "XRP Ledger"),
    Gnosis("GNO", "xDAI", "Gnosis Chain"),
    Dash("DASH", "DASH", "Dash"),
    Optimism("OPTIMISM", "ETH", "Optimistic Ethereum"),
    OptimismTestnet("OPTIMISM", "ETH", "Optimistic Ethereum Testnet"),
    Dischain("dischain", "DIS", "DisChain (ETHF)"),
    EthereumPow("ETH-Pow", "ETHW", "Ethereum PoW"),
    EthereumPowTestnet("ETH-Pow/test", "ETHW", "EthereumPoW Testnet"),
    Kaspa("KAS", "KAS", "Kaspa"),
    KaspaTestnet("KAS/test", "KAS", "Kaspa Testnet"),
    Telos("TELOS", "TLOS", "Telos EVM"),
    TelosTestnet("TELOS/test", "TLOS", "Telos Testnet"),
    TON("The-Open-Network", "TON", "TON"),
    TONTestnet("The-Open-Network/test", "TON", "Ton Testnet"),
    Ravencoin("ravencoin", "RVN", "Ravencoin"),
    RavencoinTestnet("ravencoin/test", "RVN", "Ravencoin Testnet"),
    TerraV1("terra", "LUNC", "Terra Classic"),
    TerraV2("terra-2", "LUNA", "Terra"),
    Cronos("cronos", "CRO", "Cronos EVM"),
    AlephZero("aleph-zero", "AZERO", "Aleph Zero"),
    AlephZeroTestnet("aleph-zero/test", "TZERO", "Aleph Zero Testnet"),
    OctaSpace("octaspace", "OCTA", "OctaSpace"),
    OctaSpaceTestnet("octaspace/test", "OCTA", "OctaSpace Testnet"),
    Chia("chia", "XCH", "Chia Network"),
    ChiaTestnet("chia/test", "TXCH", "Chia Network Testnet"),
    Decimal("decimal", "DEL", "Decimal Smart Chain"),
    DecimalTestnet("decimal/test", "tDEL", "Decimal Smart Chain Testnet"),
    XDC("xdc", "XDC", "XDC Network"),
    XDCTestnet("xdc/test", "XDC", "XDC Network Testnet"),
    VeChain("vechain", "VET", "VeChain"),
    VeChainTestnet("vechain/test", "VET", "VeChain Testnet"),
    Aptos("aptos", "APT", "Aptos"),
    AptosTestnet("aptos/test", "APT", "Aptos Testnet"),
    Playa3ull("playa3ull", "3ULL", "PLAYA3ULL GAMES"),
    Shibarium("shibarium", "BONE", "Shibarium"),
    ShibariumTestnet("shibarium/test", "BONE", "Shibarium Testnet"),
    Algorand("algorand", "ALGO", "Algorand"),
    AlgorandTestnet("algorand/test", "ALGO", "Algorand Testnet"),
    Hedera("hedera", "HBAR", "Hedera"),
    HederaTestnet("hedera/test", "HBAR", "Hedera Testnet"),
    Aurora("aurora", "ETH", "Aurora"),
    AuroraTestnet("aurora/test", "ETH", "Aurora Testnet"),
    Areon("areon", "AREA", "Areon Network"),
    AreonTestnet("areon/test", "TAREA", "Areon Network Testnet"),
    PulseChain("pls", "PLS", "PulseChain"),
    PulseChainTestnet("pls/test", "tPLS", "PulseChain Testnet v4"),
    ZkSyncEra("zkSyncEra", "ETH", "ZkSync Era"),
    ZkSyncEraTestnet("zkSyncEra/test", "ETH", "ZkSync Era Testnet"),
    Nexa("NEXA", "NEXA", "Nexa"),
    NexaTestnet("NEXA/test", "NEXA", "Nexa Testnet"),
    Moonbeam("moonbeam", "GLMR", "Moonbeam"),
    MoonbeamTestnet("moonbeam/test", "GLMR", "Moonbeam Testnet"),
    Manta("manta-pacific", "ETH", "Manta Pacific"),
    MantaTestnet("manta/test", "ETH", "Manta Testnet"),
    PolygonZkEVM("polygonZkEVM", "ETH", "Polygon zkEVM"),
    PolygonZkEVMTestnet("polygonZkEVM/test", "ETH", "Polygon zkEVM Testnet"),
    Radiant("radiant", "RXD", "Radiant"),
    Base("base", "ETH", "Base"),
    BaseTestnet("base/test", "ETH", "Base Testnet"),
    Moonriver("moonriver", "MOVR", "Moonriver"),
    MoonriverTestnet("moonriver/test", "MOVR", "Moonriver Testnet"),
    Mantle("mantle", "MNT", "Mantle"),
    MantleTestnet("mantle/test", "MNT", "Mantle Testnet"),
    Fact0rn("fact0rn", "FACT", "Fact0rn"),
    Flare("flare", "FLR", "Flare"),
    FlareTestnet("flare/test", "FLR", "Flare Testnet"),
    Taraxa("taraxa", "TARA", "Taraxa"),
    TaraxaTestnet("taraxa/test", "TARA", "Taraxa Testnet"),
    Koinos("koinos", "KOIN", "Koinos"),
    KoinosTestnet("koinos/test", "tKOIN", "Koinos Testnet"),
    Joystream("joystream", "JOY", "Joystream"),
    Bittensor("bittensor", "TAO", "Bittensor"),
    Filecoin("filecoin", "FIL", "Filecoin"),
    Blast("blast", "ETH", "Blast"),
    BlastTestnet("blast/test", "ETH", "Blast Testnet"),
    Cyber("cyber", "ETH", "Cyber"),
    CyberTestnet("cyber/test", "ETH", "Cyber Testnet"),
    InternetComputer("internet-computer", "ICP", "Internet Computer"),
    Sui("sui", "SUI", "Sui"),
    SuiTestnet("sui/test", "SUI", "Sui Testnet"),
    EnergyWebChain("energy-web-chain", "EWT", "Energy Web Chain"),
    EnergyWebChainTestnet("energy-web-chain/test", "VT", "Energy Web Chain Volta Testnet"),
    EnergyWebX("energy-web-x", "EWT", "Energy Web X"),
    EnergyWebXTestnet("energy-web-x/test", "VT", "Energy Web X Paseo Testnet"),
    Casper("casper", "CSPR", "Casper"),
    CasperTestnet("casper/test", "CSPR", "Casper Testnet"),
    Core("core", "CORE", "Core"),
    CoreTestnet("core/test", "tCORE", "Core Testnet"),
    Xodex("xodex", "XODEX", "Xodex"),
    Canxium("canxium", "CAU", "Canxium"),
    Chiliz("chiliz", "CHZ", "Chiliz"),
    ChilizTestnet("chiliz/test", "CHZ", "Chiliz Spicy Testnet"),
    Alephium("alephium", "ALPH", "Alephium"),
    AlephiumTestnet("alephium/test", "ALPH", "Alephium Testnet"),
    Clore("clore-ai", "CLORE", "Clore"),
    VanarChain("vanar-chain", "VANRY", "Vanar Chain"),
    VanarChainTestnet("vanar-chain/test", "VG", "Vanar Vanguard Testnet"),
    OdysseyChain("odyssey", "DIONE", "Odyssey Chain"),
    OdysseyChainTestnet("odyssey/test", "DIONE", "Odyssey Chain Testnet"),
    Bitrock("bitrock", "BROCK", "Bitrock"),
    BitrockTestnet("bitrock/test", "BROCK", "Bitrock Testnet"),
    Sonic("sonic", "S", "Sonic"),
    SonicTestnet("sonic/test", "S", "Sonic Blaze Testnet"),
    ApeChain("apechain", "APE", "ApeChain"),
    ApeChainTestnet("apechain/test", "APE", "Curtis Testnet"),
    Scroll("scroll", "ETH", "Scroll"),
    ScrollTestnet("scroll/test", "ETH", "Scroll Sepolia Testnet"),
    ZkLinkNova("zklink", "ETH", "zkLink Nova"),
    ZkLinkNovaTestnet("zklink/test", "ETH", "zkLink Nova Sepolia Testnet"),
    Pepecoin("pepecoin", "PEP", "Pepecoin"),
    PepecoinTestnet("pepecoin/test", "PEP", "Pepecoin Testnet"),
    Hyperliquid("hyperliquid", "HYPE", "Hyperliquid EVM"),
    HyperliquidTestnet("hyperliquid/test", "HYPE", "Hyperliquid EVM Testnet"),
    Quai("quai", "QUAI", "Quai"),
    QuaiTestnet("quai/test", "QUAI", "Quai Testnet"),
    Linea("linea", "ETH", "Linea"),
    LineaTestnet("linea/test", "ETH", "Linea Testnet"),
    ;

    private val externalLinkProvider: ExternalLinkProvider by lazy { ExternalLinkProviderFactory.makeProvider(this) }

    fun getCoinName(): String {
        val l2Suffix = if (isL2EthereumNetwork()) " (ETH)" else ""
        return when (this) {
            Chia -> "Chia"
            Fantom -> "Fantom"
            TON -> "Toncoin"
            OdysseyChain -> "Dione"
            OdysseyChainTestnet -> "Dione Testnet"
            ApeChain -> "ApeCoin"
            else -> this.fullName + l2Suffix
        }
    }

    fun isL2EthereumNetwork(): Boolean {
        return when (this) {
            Arbitrum,
            Optimism,
            Aurora,
            ZkSyncEra,
            Manta,
            PolygonZkEVM,
            Base,
            Blast,
            Cyber,
            Scroll,
            ZkLinkNova,
            Linea,
            -> true
            else -> false
        }
    }

    @Suppress("MagicNumber", "LongMethod")
    fun decimals(): Int = when (this) {
        Unknown -> 0

        Nexa, NexaTestnet,
        -> 2

        Cardano,
        XRP,
        Tezos,
        Tron, TronTestnet,
        Cosmos, CosmosTestnet,
        TerraV1, TerraV2,
        Algorand, AlgorandTestnet,
        Sei, SeiTestnet,
        -> 6

        Stellar, StellarTestnet -> 7

        Bitcoin, BitcoinTestnet,
        BitcoinCash, BitcoinCashTestnet,
        Binance, BinanceTestnet,
        Litecoin,
        Ducatus,
        Dogecoin,
        Dash,
        Kaspa, KaspaTestnet,
        Ravencoin, RavencoinTestnet,
        Aptos, AptosTestnet,
        Hedera, HederaTestnet,
        Radiant,
        Fact0rn,
        Koinos, KoinosTestnet,
        InternetComputer,
        Clore,
        Pepecoin, PepecoinTestnet,
        -> 8

        Solana, SolanaTestnet,
        TON, TONTestnet,
        Bittensor,
        Sui, SuiTestnet,
        Casper, CasperTestnet,
        -> 9

        Polkadot, Joystream -> 10

        PolkadotTestnet, Kusama, AlephZero, AlephZeroTestnet,
        Chia, ChiaTestnet,
        -> 12

        Arbitrum, ArbitrumTestnet,
        Ethereum, EthereumTestnet,
        EthereumClassic, EthereumClassicTestnet,
        RSK,
        BSC, BSCTestnet,
        Polygon, PolygonTestnet,
        Avalanche, AvalancheTestnet,
        Fantom, FantomTestnet,
        Gnosis,
        Optimism, OptimismTestnet,
        Dischain, EthereumPow, EthereumPowTestnet,
        Kava, KavaTestnet,
        Cronos,
        Telos, TelosTestnet,
        OctaSpace, OctaSpaceTestnet,
        Decimal, DecimalTestnet,
        XDC, XDCTestnet,
        VeChain, VeChainTestnet,
        Playa3ull,
        Shibarium, ShibariumTestnet,
        Aurora, AuroraTestnet,
        Areon, AreonTestnet,
        PulseChain, PulseChainTestnet,
        ZkSyncEra, ZkSyncEraTestnet,
        Moonbeam, MoonbeamTestnet,
        Manta, MantaTestnet,
        PolygonZkEVM, PolygonZkEVMTestnet,
        Base, BaseTestnet,
        Moonriver, MoonriverTestnet,
        Mantle, MantleTestnet,
        Flare, FlareTestnet,
        Taraxa, TaraxaTestnet,
        Filecoin,
        Blast, BlastTestnet,
        Cyber, CyberTestnet,
        EnergyWebChain, EnergyWebChainTestnet,
        EnergyWebX, EnergyWebXTestnet,
        Core, CoreTestnet,
        Chiliz, ChilizTestnet,
        VanarChain, VanarChainTestnet,
        Alephium, AlephiumTestnet,
        Xodex,
        Canxium,
        OdysseyChain, OdysseyChainTestnet,
        Bitrock, BitrockTestnet,
        Sonic, SonicTestnet,
        ApeChain, ApeChainTestnet,
        Scroll, ScrollTestnet,
        ZkLinkNova, ZkLinkNovaTestnet,
        Hyperliquid, HyperliquidTestnet,
        Quai, QuaiTestnet,
        Linea, LineaTestnet,
        -> 18

        Near, NearTestnet,
        -> 24
    }

    fun makeAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray? = null,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): Set<Address> {
        val addressService = getAddressService()
        return if (pairPublicKey != null) {
            (addressService as? MultisigAddressProvider)
                ?.makeMultisigAddresses(walletPublicKey, pairPublicKey) ?: emptySet()
        } else {
            addressService.makeAddresses(walletPublicKey, curve)
        }
    }

    fun makeAddressesFromExtendedPublicKey(
        extendedPublicKey: ExtendedPublicKey,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
        cachedIndex: Int?,
    ): EthereumDerivationData {
        val addressService = getAddressService()
        val address = addressService.makeAddressFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            curve = curve,
            derivationPath = derivationPath(DerivationStyle.V3)?.rawPath,
            cachedIndex = cachedIndex,
        )
        return address
    }

    fun validateAddress(address: String): Boolean = getAddressService().validate(address)

    fun reformatContractAddress(address: String?): String? {
        val addressService = getAddressService() as? ContractAddressValidator ?: return address
        return addressService.reformatContractAddress(address) ?: address
    }

    fun validateContractAddress(address: String): Boolean {
        val addressService = getAddressService() as? ContractAddressValidator ?: return validateAddress(address)
        return addressService.validateContractAddress(address)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun getAddressService(): AddressService {
        return when (this) {
            Bitcoin, BitcoinTestnet,
            Litecoin,
            Dogecoin,
            Ducatus,
            Dash,
            Ravencoin, RavencoinTestnet,
            Clore,
            -> BitcoinAddressService(this)

            BitcoinCash, BitcoinCashTestnet -> BitcoinCashAddressService(this)
            Arbitrum, ArbitrumTestnet,
            Ethereum, EthereumTestnet,
            EthereumClassic, EthereumClassicTestnet,
            BSC, BSCTestnet,
            Polygon, PolygonTestnet,
            Avalanche, AvalancheTestnet,
            Fantom, FantomTestnet,
            Gnosis,
            Optimism, OptimismTestnet,
            Dischain,
            EthereumPow, EthereumPowTestnet,
            Kava, KavaTestnet,
            Cronos,
            Telos, TelosTestnet,
            OctaSpace, OctaSpaceTestnet,
            VeChain, VeChainTestnet,
            Playa3ull,
            Shibarium, ShibariumTestnet,
            Aurora, AuroraTestnet,
            Areon, AreonTestnet,
            PulseChain, PulseChainTestnet,
            ZkSyncEra, ZkSyncEraTestnet,
            Moonbeam, MoonbeamTestnet,
            Manta, MantaTestnet,
            PolygonZkEVM, PolygonZkEVMTestnet,
            Base, BaseTestnet,
            Moonriver, MoonriverTestnet,
            Mantle, MantleTestnet,
            Flare, FlareTestnet,
            Taraxa, TaraxaTestnet,
            Blast, BlastTestnet,
            Cyber, CyberTestnet,
            EnergyWebChain, EnergyWebChainTestnet,
            Core, CoreTestnet,
            Chiliz, ChilizTestnet,
            VanarChain, VanarChainTestnet,
            Xodex,
            Canxium,
            OdysseyChain, OdysseyChainTestnet,
            Bitrock, BitrockTestnet,
            Sonic, SonicTestnet,
            ApeChain, ApeChainTestnet,
            Scroll, ScrollTestnet,
            ZkLinkNova, ZkLinkNovaTestnet,
            Hyperliquid, HyperliquidTestnet,
            Linea, LineaTestnet,
            -> EthereumAddressService()

            Quai, QuaiTestnet -> QuaiAddressService()

            XDC, XDCTestnet -> XDCAddressService()

            Decimal, DecimalTestnet -> DecimalAddressService()
            RSK -> RskAddressService()
            Cardano -> CardanoAddressServiceFacade()
            XRP -> XrpAddressService()
            Binance -> BinanceAddressService()
            BinanceTestnet -> BinanceAddressService(true)
            Polkadot, PolkadotTestnet,
            Kusama,
            AlephZero, AlephZeroTestnet,
            Joystream,
            Bittensor,
            EnergyWebX, EnergyWebXTestnet,
            -> PolkadotAddressService(this)
            Stellar, StellarTestnet -> StellarAddressService()
            Solana, SolanaTestnet -> SolanaAddressService()
            Tezos -> TezosAddressService()
            Sui, SuiTestnet -> SuiAddressService(this)
            Cosmos, CosmosTestnet,
            TerraV1,
            TerraV2,
            Near, NearTestnet,
            Algorand, AlgorandTestnet,
            InternetComputer,
            Filecoin,
            Sei, SeiTestnet,
            TON, TONTestnet,
            -> WalletCoreAddressService(blockchain = this)

            Aptos, AptosTestnet -> AptosAddressService(isTestnet())
            Tron, TronTestnet -> TronAddressService()
            Kaspa, KaspaTestnet -> KaspaAddressService(this.isTestnet())
            Chia, ChiaTestnet -> ChiaAddressService(this)
            Hedera, HederaTestnet -> HederaAddressService(this.isTestnet())
            Nexa, NexaTestnet -> NexaAddressService(this.isTestnet())
            Koinos, KoinosTestnet -> KoinosAddressService()
            Radiant -> RadiantAddressService()
            Fact0rn -> Fact0rnAddressService()
            Pepecoin, PepecoinTestnet -> PepecoinAddressService()
            Casper, CasperTestnet -> CasperAddressService()
            Alephium, AlephiumTestnet -> AlephiumAddressService()
            Unknown -> error("unsupported blockchain")
        }
    }

    fun getShareScheme(): List<String> = when (this) {
        Bitcoin, BitcoinTestnet -> listOf("bitcoin:")
        Ethereum, EthereumTestnet -> listOf("ethereum:", "ethereum:pay-") // "pay-" defined in ERC-681
        Litecoin -> listOf("litecoin:")
        Binance, BinanceTestnet -> listOf("bnb:")
        Dogecoin -> listOf("doge:", "dogecoin:")
        XRP -> listOf("ripple:", "xrpl:", "xrp:")
        else -> emptyList()
    }

    fun getShareUri(address: String): String = getShareScheme().firstOrNull()?.plus(address) ?: address

    fun validateShareScheme(scheme: String) = getShareScheme().any { it == "$scheme:" }

    fun getExploreUrl(address: String, tokenContractAddress: String? = null): String {
        return externalLinkProvider.explorerUrl(walletAddress = address, contractAddress = tokenContractAddress)
    }

    fun getExploreTxUrl(transactionHash: String): TxExploreState {
        return externalLinkProvider.getExplorerTxUrl(transactionHash)
    }

    fun getNFTExploreUrl(assetIdentifier: NFTAsset.Identifier): String? {
        return externalLinkProvider.getNFTExplorerUrl(assetIdentifier)
    }

    fun getTestnetTopUpUrl(): String? {
        return externalLinkProvider.testNetTopUpUrl
    }

    fun isTestnet(): Boolean = this == getTestnetVersion()

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun getTestnetVersion(): Blockchain? {
        return when (this) {
            Avalanche, AvalancheTestnet -> AvalancheTestnet
            Arbitrum, ArbitrumTestnet -> ArbitrumTestnet
            Bitcoin, BitcoinTestnet -> BitcoinTestnet
            BitcoinCash, BitcoinCashTestnet -> BitcoinCashTestnet
            Ethereum, EthereumTestnet -> EthereumTestnet
            EthereumClassic, EthereumClassicTestnet -> EthereumClassicTestnet
            Binance, BinanceTestnet -> BinanceTestnet
            BSC, BSCTestnet -> BSCTestnet
            Fantom, FantomTestnet -> FantomTestnet
            Polygon, PolygonTestnet -> PolygonTestnet
            Polkadot, PolkadotTestnet -> PolkadotTestnet
            Stellar, StellarTestnet -> StellarTestnet
            Solana, SolanaTestnet -> SolanaTestnet
            Tron, TronTestnet -> TronTestnet
            Optimism, OptimismTestnet -> OptimismTestnet
            EthereumPow, EthereumPowTestnet -> EthereumPowTestnet
            TON, TONTestnet -> TONTestnet
            Kava, KavaTestnet -> KavaTestnet
            Telos, TelosTestnet -> TelosTestnet
            Ravencoin, RavencoinTestnet -> RavencoinTestnet
            Cosmos, CosmosTestnet -> CosmosTestnet
            AlephZero, AlephZeroTestnet -> AlephZeroTestnet
            OctaSpace, OctaSpaceTestnet -> OctaSpaceTestnet
            Chia, ChiaTestnet -> ChiaTestnet
            Near, NearTestnet -> NearTestnet
            Decimal, DecimalTestnet -> DecimalTestnet
            XDC, XDCTestnet -> XDCTestnet
            VeChain, VeChainTestnet -> VeChainTestnet
            Aptos, AptosTestnet -> AptosTestnet
            Shibarium, ShibariumTestnet -> ShibariumTestnet
            Algorand, AlgorandTestnet -> AlgorandTestnet
            Hedera, HederaTestnet -> HederaTestnet
            Aurora, AuroraTestnet -> AuroraTestnet
            Areon, AreonTestnet -> AreonTestnet
            PulseChain, PulseChainTestnet -> PulseChainTestnet
            ZkSyncEra, ZkSyncEraTestnet -> ZkSyncEraTestnet
            Moonbeam, MoonbeamTestnet -> MoonbeamTestnet
            Manta, MantaTestnet -> MantaTestnet
            PolygonZkEVM, PolygonZkEVMTestnet -> PolygonZkEVMTestnet
            Base, BaseTestnet -> BaseTestnet
            Moonriver, MoonriverTestnet -> MoonriverTestnet
            Mantle, MantleTestnet -> MantleTestnet
            Flare, FlareTestnet -> FlareTestnet
            Taraxa, TaraxaTestnet -> TaraxaTestnet
            Koinos, KoinosTestnet -> KoinosTestnet
            Blast, BlastTestnet -> BlastTestnet
            Cyber, CyberTestnet -> CyberTestnet
            Sei, SeiTestnet -> SeiTestnet
            Sui, SuiTestnet -> SuiTestnet
            EnergyWebChain, EnergyWebChainTestnet -> EnergyWebChainTestnet
            EnergyWebX, EnergyWebXTestnet -> EnergyWebXTestnet
            Casper, CasperTestnet -> CasperTestnet
            Core, CoreTestnet -> CoreTestnet
            Chiliz, ChilizTestnet -> ChilizTestnet
            Alephium, AlephiumTestnet -> AlephiumTestnet
            VanarChain, VanarChainTestnet -> VanarChainTestnet
            OdysseyChain, OdysseyChainTestnet -> OdysseyChainTestnet
            Bitrock, BitrockTestnet -> BitrockTestnet
            Sonic, SonicTestnet -> SonicTestnet
            ApeChain, ApeChainTestnet -> ApeChainTestnet
            Kaspa, KaspaTestnet -> KaspaTestnet
            ZkLinkNova, ZkLinkNovaTestnet -> ZkLinkNovaTestnet
            Nexa, NexaTestnet -> NexaTestnet
            Scroll, ScrollTestnet -> ScrollTestnet
            Pepecoin, PepecoinTestnet -> PepecoinTestnet
            Hyperliquid, HyperliquidTestnet -> HyperliquidTestnet
            Quai, QuaiTestnet -> QuaiTestnet
            Linea, LineaTestnet -> LineaTestnet
            Unknown,
            Cardano,
            Dogecoin,
            Ducatus,
            Litecoin,
            Kusama,
            RSK,
            Tezos,
            XRP,
            Gnosis,
            Dash,
            Dischain,
            TerraV1,
            TerraV2,
            Cronos,
            Playa3ull,
            Radiant,
            Fact0rn,
            Joystream,
            Bittensor,
            Filecoin,
            InternetComputer,
            Xodex,
            Canxium,
            Clore,
            -> null
        }
    }

    @Suppress("LongMethod")
    fun getSupportedCurves(): List<EllipticCurve> {
        return when (this) {
            Unknown -> emptyList()
            Tezos,
            -> listOf(
                EllipticCurve.Secp256k1,
                EllipticCurve.Ed25519,
                EllipticCurve.Ed25519Slip0010,
            )

            XRP,
            -> listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)

            Arbitrum, ArbitrumTestnet,
            Bitcoin, BitcoinTestnet,
            BitcoinCash, BitcoinCashTestnet,
            Binance, BinanceTestnet,
            Ethereum, EthereumTestnet,
            Kava, KavaTestnet,
            Telos, TelosTestnet,
            EthereumClassic, EthereumClassicTestnet,
            Polygon, PolygonTestnet,
            Avalanche, AvalancheTestnet,
            BSC, BSCTestnet,
            Fantom, FantomTestnet,
            Litecoin,
            Ducatus,
            RSK,
            Dogecoin,
            Tron, TronTestnet,
            Gnosis,
            Dash,
            Optimism, OptimismTestnet,
            Dischain, EthereumPow, EthereumPowTestnet,
            Kaspa, KaspaTestnet,
            Ravencoin, RavencoinTestnet,
            Cosmos, CosmosTestnet,
            TerraV1, TerraV2,
            Cronos,
            OctaSpace, OctaSpaceTestnet,
            Decimal, DecimalTestnet,
            XDC, XDCTestnet,
            VeChain, VeChainTestnet,
            Playa3ull,
            Shibarium, ShibariumTestnet,
            Aurora, AuroraTestnet,
            Areon, AreonTestnet,
            PulseChain, PulseChainTestnet,
            ZkSyncEra, ZkSyncEraTestnet,
            Nexa, NexaTestnet,
            Moonbeam, MoonbeamTestnet,
            Manta, MantaTestnet,
            PolygonZkEVM, PolygonZkEVMTestnet,
            Radiant,
            Fact0rn,
            Base, BaseTestnet,
            Moonriver, MoonriverTestnet,
            Mantle, MantleTestnet,
            Flare, FlareTestnet,
            Taraxa, TaraxaTestnet,
            Koinos, KoinosTestnet,
            Filecoin,
            Blast, BlastTestnet,
            Cyber, CyberTestnet,
            Sei, SeiTestnet,
            InternetComputer,
            EnergyWebChain, EnergyWebChainTestnet,
            Core, CoreTestnet,
            Casper, CasperTestnet,
            Chiliz, ChilizTestnet,
            Alephium, AlephiumTestnet,
            VanarChain, VanarChainTestnet,
            Xodex,
            Canxium,
            Clore,
            OdysseyChain, OdysseyChainTestnet,
            Bitrock, BitrockTestnet,
            Sonic, SonicTestnet,
            ApeChain, ApeChainTestnet,
            Scroll, ScrollTestnet,
            ZkLinkNova, ZkLinkNovaTestnet,
            Pepecoin, PepecoinTestnet,
            Hyperliquid, HyperliquidTestnet,
            Quai, QuaiTestnet,
            Linea, LineaTestnet,
            -> listOf(EllipticCurve.Secp256k1)

            Stellar, StellarTestnet,
            Solana, SolanaTestnet,
            Polkadot, PolkadotTestnet,
            Kusama,
            AlephZero, AlephZeroTestnet,
            Joystream,
            Bittensor,
            TON, TONTestnet,
            Near, NearTestnet,
            Aptos, AptosTestnet,
            Algorand, AlgorandTestnet,
            Hedera, HederaTestnet,
            Sui, SuiTestnet,
            EnergyWebX, EnergyWebXTestnet,
            -> listOf(EllipticCurve.Ed25519, EllipticCurve.Ed25519Slip0010)

            Cardano -> listOf(EllipticCurve.Ed25519) // todo until cardano support in wallet 2

            Chia, ChiaTestnet,
            -> listOf(EllipticCurve.Bls12381G2Aug)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun getChainId(): Int? {
        return when (this) {
            Arbitrum -> Chain.Arbitrum.id
            ArbitrumTestnet -> Chain.ArbitrumTestnet.id
            Avalanche -> Chain.Avalanche.id
            AvalancheTestnet -> Chain.AvalancheTestnet.id
            Ethereum -> Chain.Mainnet.id
            EthereumTestnet -> Chain.EthereumSepoliaTestnet.id
            EthereumClassic -> Chain.EthereumClassicMainnet.id
            EthereumClassicTestnet -> Chain.EthereumClassicTestnet.id
            Fantom -> Chain.Fantom.id
            FantomTestnet -> Chain.FantomTestnet.id
            RSK -> Chain.RskMainnet.id
            BSC -> Chain.BscMainnet.id
            BSCTestnet -> Chain.BscTestnet.id
            Polygon -> Chain.Polygon.id
            PolygonTestnet -> Chain.PolygonTestnet.id
            Gnosis -> Chain.Gnosis.id
            Optimism -> Chain.Optimism.id
            OptimismTestnet -> Chain.OptimismTestnet.id
            Dischain -> Chain.EthereumFair.id
            EthereumPow -> Chain.EthereumPow.id
            EthereumPowTestnet -> Chain.EthereumPowTestnet.id
            Kava -> Chain.Kava.id
            KavaTestnet -> Chain.KavaTestnet.id
            Telos -> Chain.Telos.id
            TelosTestnet -> Chain.TelosTestnet.id
            Cronos -> Chain.Cronos.id
            OctaSpace -> Chain.OctaSpace.id
            OctaSpaceTestnet -> Chain.OctaSpaceTestnet.id
            Decimal -> Chain.Decimal.id
            DecimalTestnet -> Chain.DecimalTestnet.id
            XDC -> Chain.Xdc.id
            XDCTestnet -> Chain.XdcTestnet.id
            Playa3ull -> Chain.Playa3ull.id
            Shibarium -> Chain.Shibarium.id
            ShibariumTestnet -> Chain.ShibariumTestnet.id
            Aurora -> Chain.Aurora.id
            AuroraTestnet -> Chain.AuroraTestnet.id
            Areon -> Chain.Areon.id
            AreonTestnet -> Chain.AreonTestnet.id
            PulseChain -> Chain.PulseChain.id
            PulseChainTestnet -> Chain.PulseChainTestnet.id
            ZkSyncEra -> Chain.ZkSyncEra.id
            ZkSyncEraTestnet -> Chain.ZkSyncEraTestnet.id
            Moonbeam -> Chain.Moonbeam.id
            MoonbeamTestnet -> Chain.MoonbeamTestnet.id
            Manta -> Chain.Manta.id
            MantaTestnet -> Chain.MantaTestnet.id
            PolygonZkEVM -> Chain.PolygonZkEVM.id
            PolygonZkEVMTestnet -> Chain.PolygonZkEVMTestnet.id
            Base -> Chain.Base.id
            BaseTestnet -> Chain.BaseTestnet.id
            Moonriver -> Chain.Moonriver.id
            MoonriverTestnet -> Chain.MoonriverTestnet.id
            Mantle -> Chain.Mantle.id
            MantleTestnet -> Chain.MantleTestnet.id
            Flare -> Chain.Flare.id
            FlareTestnet -> Chain.FlareTestnet.id
            Taraxa -> Chain.Taraxa.id
            TaraxaTestnet -> Chain.TaraxaTestnet.id
            Blast -> Chain.Blast.id
            BlastTestnet -> Chain.BlastTestnet.id
            Cyber -> Chain.Cyber.id
            CyberTestnet -> Chain.CyberTestnet.id
            EnergyWebChain -> Chain.EnergyWebChain.id
            EnergyWebChainTestnet -> Chain.EnergyWebChainTestnet.id
            Core -> Chain.Core.id
            CoreTestnet -> Chain.CoreTestnet.id
            Xodex -> Chain.Xodex.id
            Canxium -> Chain.Canxium.id
            Chiliz -> Chain.Chiliz.id
            VanarChain -> Chain.VanarChain.id
            VanarChainTestnet -> Chain.VanarChainTestnet.id
            ChilizTestnet -> Chain.ChilizTestnet.id
            OdysseyChain -> Chain.OdysseyChain.id
            OdysseyChainTestnet -> Chain.OdysseyChainTestnet.id
            Bitrock -> Chain.Bitrock.id
            BitrockTestnet -> Chain.BitrockTestnet.id
            Sonic -> Chain.Sonic.id
            SonicTestnet -> Chain.SonicTestnet.id
            ApeChain -> Chain.ApeChain.id
            ApeChainTestnet -> Chain.ApeChainTestnet.id
            Scroll -> Chain.Scroll.id
            ScrollTestnet -> Chain.ScrollTestnet.id
            ZkLinkNova -> Chain.ZkLinkNova.id
            ZkLinkNovaTestnet -> Chain.ZkLinkNovaTestnet.id
            Pepecoin -> Chain.Pepecoin.id
            PepecoinTestnet -> Chain.PepecoinTestnet.id
            Hyperliquid -> Chain.Hyperliquid.id
            HyperliquidTestnet -> Chain.HyperliquidTestnet.id
            Quai -> Chain.Quai.id
            QuaiTestnet -> Chain.QuaiTestnet.id
            Linea -> Chain.Linea.id
            LineaTestnet -> Chain.LineaTestnet.id
            else -> null
        }
    }

    fun derivationPath(style: DerivationStyle?): DerivationPath? {
        if (style == null) return null
        val supportedCurves = getSupportedCurves()
        if (EllipticCurve.Secp256k1 !in supportedCurves &&
            EllipticCurve.Ed25519 !in supportedCurves &&
            EllipticCurve.Ed25519Slip0010 !in supportedCurves
        ) {
            return null
        }
        return style.getConfig().derivations(this).values.first()
    }

    fun canHandleTokens(): Boolean {
        // disable tokens support for Taraxa evm until it's not tested
        if (this == Taraxa || this == EnergyWebChain) return false

        if (isEvm()) return true

        return when (this) {
            Binance, BinanceTestnet,
            Solana, SolanaTestnet,
            Tron, TronTestnet,
            TerraV1,
            VeChain, VeChainTestnet,
            Hedera, HederaTestnet,
            TON, TONTestnet,
            Cardano,
            Kaspa, KaspaTestnet,
            Sui, SuiTestnet,
            Stellar, StellarTestnet,
            XRP,
            -> true

            else -> false
        }
    }

    fun canHandleNFTs(): Boolean = when (this) {
        // EVM
        Ethereum, // supported testnet - Sepolia (11155111)
        Arbitrum, // supported testnet - Sepolia (421614)
        Avalanche,
        Fantom, FantomTestnet,
        BSC, BSCTestnet,
        Polygon, // supported testnet - Amoy (80002)
        Cronos,
        Moonbeam, MoonbeamTestnet,
        Moonriver, MoonriverTestnet,
        Chiliz, ChilizTestnet,
        Optimism, // supported testnet - Sepolia (11155420)
        Base, BaseTestnet,

        Solana,
        -> true

        else -> false
    }

    fun isEvm(): Boolean = getChainId() != null

    fun isFeeApproximate(amountType: AmountType): Boolean = when {
        this in listOf(
            Stellar, StellarTestnet,
            TON, TONTestnet,
            Near, NearTestnet,
            Aptos, AptosTestnet,
            Hedera, HederaTestnet,
            Koinos, KoinosTestnet,
        ) -> true
        this in listOf(
            Tron,
            TronTestnet,
            VeChain,
            VeChainTestnet,
        ) -> amountType is AmountType.Token
        isEvm() -> true
        else -> false
    }

    fun feePaidCurrency(): FeePaidCurrency = when (this) {
        VeChain, VeChainTestnet -> FeePaidCurrency.Token(VeChainWalletManager.VTHO_TOKEN)
        TerraV1 -> FeePaidCurrency.SameCurrency
        Koinos, KoinosTestnet -> FeePaidCurrency.FeeResource("Mana")
        else -> FeePaidCurrency.Coin
    }

    /**
     * List of supported blockchains for generating XPUB with BIP44 derivation.
     * @see <a href="https://iancoleman.io/bip39/">bip39</a>
     */
    fun isBip44DerivationStyleXPUB(): Boolean = when (this) {
        Bitcoin, BitcoinTestnet,
        BitcoinCash, BitcoinCashTestnet,
        Litecoin,
        Dogecoin,
        Dash,
        Kaspa, KaspaTestnet,
        Ravencoin, RavencoinTestnet,
        Ducatus,
        -> true
        else -> false
    }

    /**
     * Returns is fee in given network is zero
     * For now actual only for one network
     */
    fun isNetworkFeeZero(): Boolean {
        return when (this) {
            Xodex -> true
            else -> false
        }
    }

    companion object {
        private val values = values()

        fun fromId(id: String): Blockchain = values.find { it.id == id } ?: Unknown

        fun fromChainId(chainId: Int): Blockchain? = Chain.values()
            .find { it.id == chainId }?.blockchain

        fun fromCurve(curve: EllipticCurve): List<Blockchain> = values
            .filter { it.getSupportedCurves().contains(curve) }

        fun secp256k1Blockchains(isTestnet: Boolean): List<Blockchain> = values
            .filter { it.isTestnet() == isTestnet }
            .filter { it.getSupportedCurves().contains(EllipticCurve.Secp256k1) }

        fun secp256k1OnlyBlockchains(isTestnet: Boolean): List<Blockchain> = values
            .filter { it.isTestnet() == isTestnet }
            .filter { it.getSupportedCurves().size == 1 }
            .filter { it.getSupportedCurves()[0] == EllipticCurve.Secp256k1 }

        fun ed25519OnlyBlockchains(isTestnet: Boolean): List<Blockchain> = values
            .filter { it.isTestnet() == isTestnet }
            .filter { it.getSupportedCurves().size == 1 }
            .filter { it.getSupportedCurves()[0] == EllipticCurve.Ed25519 }

        fun ed25519Blockchains(isTestnet: Boolean): List<Blockchain> = values
            .filter {
                it.isTestnet() == isTestnet && it.getSupportedCurves().contains(EllipticCurve.Ed25519)
            }

        /**
         * List of supported blockchains for activating yield module
         */
        fun yieldSupplySupportedBlockchains(): List<Blockchain> {
            return listOf(
                Polygon,
                EthereumTestnet,
            )
        }
    }
}