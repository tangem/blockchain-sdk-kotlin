package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.kaspa.KaspaAddressService
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.*
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.BIP44
import com.tangem.crypto.hdWallet.DerivationNode
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
    Cardano("CARDANO", "ADA", "Cardano"),
    CardanoShelley("CARDANO-S", "ADA", "Cardano"),
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
    ;

    fun decimals(): Int = when (this) {
        Unknown -> 0
        Cardano, CardanoShelley,
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
        PolkadotTestnet, Kusama, AlephZero, AlephZeroTestnet -> 12
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
        -> 18
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
        } else if (addressService is MultipleAddressProvider) {
            addressService.makeAddresses(walletPublicKey, curve)
        } else {
            setOf(PlainAddress(addressService.makeAddress(walletPublicKey, curve)))
        }
    }

    fun validateAddress(address: String): Boolean = getAddressService().validate(address)

    internal fun getAddressService(): AddressService {
        return when (this) {
            Bitcoin, BitcoinTestnet,
            Litecoin,
            Dogecoin,
            Ducatus,
            Dash,
            Ravencoin, RavencoinTestnet,
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
            EthereumFair,
            EthereumPow, EthereumPowTestnet,
            Kava, KavaTestnet,
            Cronos,
            Telos, TelosTestnet,
            -> EthereumAddressService()
            RSK -> RskAddressService()
            Cardano, CardanoShelley -> CardanoAddressService(this)
            XRP -> XrpAddressService()
            Binance -> BinanceAddressService()
            BinanceTestnet -> BinanceAddressService(true)
            Polkadot, PolkadotTestnet, Kusama, AlephZero, AlephZeroTestnet -> PolkadotAddressService(this)
            Stellar, StellarTestnet -> StellarAddressService()
            Solana, SolanaTestnet -> SolanaAddressService()
            Tezos -> TezosAddressService()
            TON, TONTestnet, Cosmos, CosmosTestnet, TerraV1, TerraV2 -> TrustWalletAddressService(blockchain = this)
            Tron, TronTestnet -> TronAddressService()
            Kaspa -> KaspaAddressService()
            Unknown -> throw Exception("unsupported blockchain")
        }
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

    private fun getBaseExploreUrl(): String = when (this) {
        Arbitrum -> "https://arbiscan.io/"
        ArbitrumTestnet -> "https://goerli-rollup-explorer.arbitrum.io/"
        Avalanche -> "https://snowtrace.io/"
        AvalancheTestnet -> "https://testnet.snowtrace.io/"
        Binance -> "https://explorer.binance.org/"
        BinanceTestnet -> "https://testnet-explorer.binance.org/"
        Bitcoin -> "https://www.blockchair.com/bitcoin/"
        BitcoinTestnet -> "https://www.blockchair.com/bitcoin/testnet/"
        BitcoinCash -> "https://www.blockchair.com/bitcoin-cash/"
        BitcoinCashTestnet -> "https://www.blockchain.com/bch-testnet/"
        BSC -> "https://bscscan.com/"
        BSCTestnet -> "https://testnet.bscscan.com/"
        Cardano, CardanoShelley -> "https://www.blockchair.com/cardano/"
        Dogecoin -> "https://blockchair.com/dogecoin/"
        Ducatus -> "https://insight.ducatus.io/#/DUC/mainnet/"
        Ethereum -> "https://etherscan.io/"
        EthereumTestnet -> "https://goerli.etherscan.io/"
        EthereumClassic -> "https://blockscout.com/etc/mainnet/"
        EthereumClassicTestnet -> "https://blockscout.com/etc/kotti/"
        Fantom -> "https://ftmscan.com/"
        FantomTestnet -> "https://testnet.ftmscan.com/"
        Litecoin -> "https://blockchair.com/litecoin/"
        Polkadot -> "https://polkadot.subscan.io/"
        PolkadotTestnet -> "https://westend.subscan.io/"
        Kusama -> "https://kusama.subscan.io/"
        Polygon -> "https://polygonscan.com/"
        PolygonTestnet -> "https://explorer-mumbai.maticvigil.com/"
        RSK -> "https://explorer.rsk.co/"
        Stellar -> "https://stellar.expert/explorer/public/"
        StellarTestnet -> "https://stellar.expert/explorer/testnet/"
        Solana -> "https://explorer.solana.com/"
        SolanaTestnet -> "https://explorer.solana.com/"
        Tezos -> "https://tzkt.io/"
        Telos -> "https://teloscan.io/"
        TelosTestnet -> "https://testnet.teloscan.io/"
        TON -> "https://tonscan.org/"
        TONTestnet -> "https://testnet.tonscan.org/"
        Tron -> "https://tronscan.org/#/"
        TronTestnet -> "https://nile.tronscan.org/#/"
        XRP -> "https://xrpscan.com/"
        Gnosis -> "https://blockscout.com/xdai/mainnet/"
        Dash -> "https://blockexplorer.one/dash/mainnet/"
        Optimism -> "https://optimistic.etherscan.io/"
        OptimismTestnet -> "https://blockscout.com/optimism/goerli/"
        EthereumFair -> "https://explorer.etherfair.org/"
        EthereumPow -> "https://mainnet.ethwscan.com/"
        EthereumPowTestnet -> "https://iceberg.ethwscan.com/"
        Kaspa -> "https://explorer.kaspa.org/"
        Kava -> "https://explorer.kava.io/"
        KavaTestnet -> "https://explorer.testnet.kava.io/"
        Ravencoin -> "https://api.ravencoin.org/"
        RavencoinTestnet -> "https://testnet.ravencoin.network/"
        CosmosTestnet -> "https://explorer.theta-testnet.polypore.xyz/accounts/"
        Cosmos -> "https://www.mintscan.io/cosmos/account/"
        TerraV1 -> "https://finder.terra.money/classic/"
        TerraV2 -> "https://terrasco.pe/mainnet/"
        Cronos -> "https://cronoscan.com/"
        AlephZero -> "https://alephzero.subscan.io/"
        AlephZeroTestnet -> throw Exception("unsupported blockchain")
        Unknown -> throw Exception("unsupported blockchain")
    }

    fun getExploreUrl(address: String, tokenContractAddress: String? = null): String {
        val path = "address/$address"
        val baseUrl = getBaseExploreUrl()
        val fullUrl = baseUrl + path
        return when (this) {
            Ethereum, EthereumTestnet -> if (tokenContractAddress == null) {
                fullUrl
            } else {
                "$baseUrl$tokenContractAddress?a=$address"
            }
            EthereumClassic, EthereumClassicTestnet,
            Kava, KavaTestnet,
            -> "$fullUrl/transactions"
            RSK -> if (tokenContractAddress != null) {
                "$fullUrl?__tab=tokens"
            } else {
                fullUrl
            }
            SolanaTestnet -> "$fullUrl/?cluster=devnet"
            XRP, Stellar, StellarTestnet -> "${baseUrl}account/$address"
            Tezos -> "$baseUrl$address"
            Kaspa -> "${baseUrl}addresses/$address"
            Cosmos -> "${baseUrl}$address"
            CosmosTestnet -> "${baseUrl}$address"
            else -> fullUrl
        }
    }

    fun getExploreTxUrl(transaction: String): String {
        val url = getBaseExploreUrl() + "tx/$transaction"
        return when (this) {
            SolanaTestnet -> "$url/?cluster=devnet"
            else -> url
        }
    }

    fun getTestnetTopUpUrl(): String? {
        return when (this) {
            AvalancheTestnet -> "https://faucet.avax-test.network/"
            BitcoinTestnet -> "https://coinfaucet.eu/en/btc-testnet/"
            EthereumTestnet -> "https://goerlifaucet.com/"
            EthereumClassicTestnet -> "https://kottifaucet.me"
            BitcoinCashTestnet -> "https://coinfaucet.eu/en/bch-testnet/"
            BinanceTestnet -> "https://docs.binance.org/smart-chain/wallet/binance.html"
            BSCTestnet -> "https://testnet.binance.org/faucet-smart"
            FantomTestnet -> "https://faucet.fantom.network"
            PolygonTestnet -> "https://faucet.matic.network"
            PolkadotTestnet -> "https://app.element.io/#/room/#westend_faucet:matrix.org"
            StellarTestnet -> "https://laboratory.stellar.org/#account-creator?network=test"
            SolanaTestnet -> "https://solfaucet.com/"
            TronTestnet -> "https://nileex.io/join/getJoinPage"
            OptimismTestnet -> "https://optimismfaucet.xyz" //another one https://faucet.paradigm.xyz
            EthereumPowTestnet -> "https://faucet.ethwscan.com"
            KavaTestnet -> "https://faucet.kava.io"
            TelosTestnet -> "https://app.telos.net/testnet/developers"
            CosmosTestnet -> "https://discord.com/channels/669268347736686612/953697793476821092"
            AlephZeroTestnet -> "https://faucet.test.azero.dev/"
            else -> null
        }
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
            Unknown,
            Cardano,
            CardanoShelley,
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
            Ducatus
            -> {
                null // there is no testnet for given network
            }
        }
    }

    fun getSupportedCurves(): List<EllipticCurve> {
        return when (this) {
            Unknown -> emptyList()
            Tezos,
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
            -> listOf(EllipticCurve.Secp256k1)
            Stellar, StellarTestnet,
            Solana, SolanaTestnet,
            Cardano,
            CardanoShelley,
            Polkadot, PolkadotTestnet, Kusama, AlephZero, AlephZeroTestnet,
            TON, TONTestnet,
            -> listOf(EllipticCurve.Ed25519)
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
            else -> null
        }
    }

    fun derivationPath(style: DerivationStyle?): DerivationPath? {
        if (style == null) return null
        if (!getSupportedCurves().contains(EllipticCurve.Secp256k1) &&
            !getSupportedCurves().contains(EllipticCurve.Ed25519)
        ) {
            return null
        }

        return when (this) {
            Stellar, StellarTestnet, Solana, SolanaTestnet -> {
                //Path according to sep-0005. https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0005.md
                // Solana path consistent with TrustWallet:
                // https://github.com/trustwallet/wallet-core/blob/456f22d6a8ce8a66ccc73e3b42bcfec5a6afe53a/registry.json#L1013
                DerivationPath(
                    path = listOf(
                        DerivationNode.Hardened(BIP44.purpose),
                        DerivationNode.Hardened(CoinTypeProvider.getCoinType(this, style)),
                        DerivationNode.Hardened(0)
                    )
                )
            }
            CardanoShelley -> { //We use shelley for all new cards with HD wallets feature
                //Path according to CIP-1852. https://cips.cardano.org/cips/cip1852/
                DerivationPath(
                    path = listOf(
                        DerivationNode.Hardened(1852),
                        DerivationNode.Hardened(CoinTypeProvider.getCoinType(this, style)),
                        DerivationNode.Hardened(0),
                        DerivationNode.NonHardened(0),
                        DerivationNode.NonHardened(0)
                    )
                )
            }
            AlephZero, AlephZeroTestnet -> {
                DerivationPath(
                    path = listOf(
                        DerivationNode.Hardened(BIP44.purpose),
                        DerivationNode.Hardened(CoinTypeProvider.getCoinType(this, style)),
                        DerivationNode.Hardened(0),
                        DerivationNode.Hardened(0),
                        DerivationNode.Hardened(0)
                    )
                )
            }
            else -> {
                // Standard BIP44
                val bip44 = BIP44(
                    coinType = CoinTypeProvider.getCoinType(this, style),
                    account = 0,
                    change = BIP44.Chain.External,
                    addressIndex = 0
                )
                bip44.buildPath()
            }
        }
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
        Cronos,
        -> amountType is AmountType.Token
        Arbitrum, ArbitrumTestnet,
        Optimism, OptimismTestnet,
        TON, TONTestnet,
        -> true
        else -> false
    }

    fun tokenTransactionFeePaidInNetworkCurrency(): Boolean = when (this) {
        TerraV1 -> true
        else -> false
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
    }
}
