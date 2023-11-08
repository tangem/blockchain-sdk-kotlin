package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressServiceFacade
import com.tangem.blockchain.blockchains.chia.ChiaAddressService
import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.common.address.*
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.kaspa.KaspaAddressService
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.externallinkprovider.ExternalLinkProvider
import com.tangem.blockchain.externallinkprovider.ExternalLinkProviderFactory
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath

enum class Blockchain(
    val id: String,
    val currency: String,
    val fullName: String,
) {
    Unknown("", "", ""),
    Arbitrum("ARBITRUM-ONE", "ETH", "Arbitrum"),
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
    Fantom("FTM", "FTM", "Fantom"),
    FantomTestnet("FTM/test", "FTM", "Fantom Testnet"),
    Litecoin("LTC", "LTC", "Litecoin"),
    Near("NEAR", "NEAR", "NEAR Protocol"),
    NearTestnet("NEAR/test", "NEAR", "NEAR Protocol Testnet"),
    Polkadot("Polkadot", "DOT", "Polkadot"),
    PolkadotTestnet("Polkadot", "WND", "Polkadot Westend Testnet"),
    Kava("KAVA", "KAVA", "Kava EVM"),
    KavaTestnet("KAVA/test", "KAVA", "Kava EVM Testnet"),
    Kusama("Kusama", "KSM", "Kusama"),
    Polygon("POLYGON", "MATIC", "Polygon"),
    PolygonTestnet("POLYGON/test", "MATIC", "Polygon Testnet"),
    RSK("RSK", "RBTC", "RSK"),
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
    EthereumFair("ETH-Fair", "ETF", "EthereumFair"),
    EthereumPow("ETH-Pow", "ETHW", "EthereumPoW"),
    EthereumPowTestnet("ETH-Pow/test", "ETHW", "EthereumPoW Testnet"),
    Kaspa("KAS", "KAS", "Kaspa"),
    Telos("TELOS", "TLOS", "Telos"),
    TelosTestnet("TELOS/test", "TLOS", "Telos Testnet"),
    TON("The-Open-Network", "TON", "Ton"),
    TONTestnet("The-Open-Network/test", "TON", "Ton Testnet"),
    Ravencoin("ravencoin", "RVN", "Ravencoin"),
    RavencoinTestnet("ravencoin/test", "RVN", "Ravencoin Testnet"),
    TerraV1("terra", "LUNC", "Terra Classic"),
    TerraV2("terra-2", "LUNA", "Terra"),
    Cronos("cronos", "CRO", "Cronos"),
    AlephZero("aleph-zero", "AZERO", "Aleph Zero"),
    AlephZeroTestnet("aleph-zero/test", "TZERO", "Aleph Zero Testnet"),
    OctaSpace("octaspace", "OCTA", "OctaSpace"),
    OctaSpaceTestnet("octaspace/test", "OCTA", "OctaSpace Testnet"),
    Chia("chia", "XCH", "Chia Network"),
    ChiaTestnet("chia/test", "TXCH", "Chia Network Testnet"),
    Decimal("decimal", "DEL", "Decimal Smart Chain"),
    DecimalTestnet("decimal/test", "tDEL", "Decimal Smart Chain Testnet"),
    ;

    private val externalLinkProvider: ExternalLinkProvider by lazy { ExternalLinkProviderFactory.makeProvider(this) }

    fun decimals(): Int = when (this) {
        Unknown -> 0

        Cardano,
        XRP,
        Tezos,
        Tron, TronTestnet,
        Cosmos, CosmosTestnet,
        TerraV1, TerraV2,
        -> 6

        Stellar, StellarTestnet -> 7

        Bitcoin, BitcoinTestnet,
        BitcoinCash, BitcoinCashTestnet,
        Binance, BinanceTestnet,
        Litecoin,
        Ducatus,
        Dogecoin,
        Dash,
        Kaspa,
        Ravencoin, RavencoinTestnet,
        -> 8

        Solana, SolanaTestnet,
        TON, TONTestnet,
        -> 9

        Polkadot -> 10

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
        EthereumFair, EthereumPow, EthereumPowTestnet,
        Kava, KavaTestnet,
        Cronos,
        Telos, TelosTestnet,
        OctaSpace, OctaSpaceTestnet,
        Decimal, DecimalTestnet,
        -> 18

        Near, NearTestnet,
        -> 24
    }

    fun derivationPaths(style: DerivationStyle): Map<AddressType, DerivationPath> {
        val curves = getSupportedCurves()

        // add BLS later
        if (!curves.contains(EllipticCurve.Secp256k1) && !curves.contains(EllipticCurve.Ed25519)) {
            return emptyMap()
        }

        return style.provider().derivations(this)
    }

    fun validateAddress(address: String): Boolean {
        return AddressServiceFactory(this).makeAddressService().validate(address)
    }

    fun getShareScheme(): String? = when (this) {
        Bitcoin, BitcoinTestnet -> "bitcoin"
        Ethereum, EthereumTestnet -> "ethereum"
        Litecoin -> "litecoin"
        Binance, BinanceTestnet -> "bnb"
        else -> null
    }

    fun getShareUri(address: String): String = getShareScheme()?.plus(":$address") ?: address

    fun validateShareScheme(scheme: String): Boolean {
        if (this == XRP && (scheme == "ripple" || scheme == "xrpl" || scheme == "xrp")) return true
        return scheme == getShareScheme()
    }

    fun getExploreUrl(address: String, tokenContractAddress: String? = null): String {
        return externalLinkProvider.explorerUrl(walletAddress = address, contractAddress = tokenContractAddress)
    }

    fun getExploreTxUrl(transactionHash: String): String {
        return externalLinkProvider.explorerTransactionUrl(transactionHash)
    }

    fun getTestnetTopUpUrl(): String? {
        return externalLinkProvider.testNetTopUpUrl
    }

    fun isTestnet(): Boolean = this == getTestnetVersion()

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
            Unknown,
            Cardano,
            Dogecoin,
            Litecoin,
            Kusama,
            RSK,
            Tezos,
            XRP,
            Gnosis,
            Dash,
            EthereumFair,
            Kaspa,
            TerraV1,
            TerraV2,
            Cronos,
            Ducatus,
            -> {
                null // there is no testnet for given network
            }
        }
    }

    fun getSupportedCurves(): List<EllipticCurve> {
        return when (this) {
            Unknown -> emptyList()
            Tezos,
            -> listOf(
                EllipticCurve.Secp256k1,
                EllipticCurve.Ed25519,
                EllipticCurve.Ed25519Slip0010
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
            EthereumFair, EthereumPow, EthereumPowTestnet,
            Kaspa,
            Ravencoin, RavencoinTestnet,
            Cosmos, CosmosTestnet,
            TerraV1, TerraV2,
            Cronos,
            OctaSpace, OctaSpaceTestnet,
            Decimal, DecimalTestnet,
            -> listOf(EllipticCurve.Secp256k1)

            Stellar, StellarTestnet,
            Solana, SolanaTestnet,
            Polkadot, PolkadotTestnet, Kusama, AlephZero, AlephZeroTestnet,
            TON, TONTestnet, Near, NearTestnet,
            -> listOf(EllipticCurve.Ed25519, EllipticCurve.Ed25519Slip0010)

            Cardano -> listOf(EllipticCurve.Ed25519) //todo until cardano support in wallet 2
            Chia, ChiaTestnet,
            -> listOf(EllipticCurve.Bls12381G2Aug)
        }
    }

    fun getChainId(): Int? {
        return when (this) {
            Arbitrum -> Chain.Arbitrum.id
            ArbitrumTestnet -> Chain.ArbitrumTestnet.id
            Avalanche -> Chain.Avalanche.id
            AvalancheTestnet -> Chain.AvalancheTestnet.id
            Ethereum -> Chain.Mainnet.id
            EthereumTestnet -> Chain.Goerli.id
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
            EthereumFair -> Chain.EthereumFair.id
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
            else -> null
        }
    }

    fun derivationPath(style: DerivationStyle?): DerivationPath? {
        if (style == null) return null
        if (!getSupportedCurves().contains(EllipticCurve.Secp256k1) &&
            !getSupportedCurves().contains(EllipticCurve.Ed25519) &&
            !getSupportedCurves().contains(EllipticCurve.Ed25519Slip0010)
        ) {
            return null
        }
        return style.provider().derivations(this).values.first()
    }

    fun canHandleTokens(): Boolean {
        if (isEvm()) return true

        return when (this) {
            Binance, BinanceTestnet,
            Solana, SolanaTestnet,
            Tron, TronTestnet,
            TerraV1,
            -> true

            else -> false
        }
    }

    fun isEvm(): Boolean = getChainId() != null

    fun isFeeApproximate(amountType: AmountType): Boolean = when (this) {
        Fantom, FantomTestnet,
        Tron, TronTestnet,
        Avalanche, AvalancheTestnet,
        EthereumPow,
        Cronos,
        -> amountType is AmountType.Token

        Arbitrum, ArbitrumTestnet,
        Stellar, StellarTestnet,
        Optimism, OptimismTestnet,
        TON, TONTestnet,
        Near, NearTestnet
        -> true

        else -> false
    }

    fun tokenTransactionFeePaidInNetworkCurrency(): Boolean = when (this) {
        TerraV1 -> true
        else -> false
    }

    fun getPrimaryCurve(): EllipticCurve? {
        return when {
            getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }

            getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }

            else -> {
                null
            }
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


        fun valuesWithoutUnknown(): List<Blockchain> {
            return Blockchain.values().toMutableList().apply { remove(Unknown) }.toList()
        }

        fun ed25519Blockchains(isTestnet: Boolean): List<Blockchain> = values
            .filter {
                it.isTestnet() == isTestnet && it.getSupportedCurves().contains(EllipticCurve.Ed25519)
            }

    }
}
