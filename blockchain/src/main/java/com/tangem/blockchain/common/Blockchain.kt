package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressType
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.*
import com.tangem.common.card.EllipticCurve
import com.tangem.common.hdWallet.DerivationNode
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.bip.BIP44


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
    EthereumPowTestnet("ETH-Pow/test", "ETHW", "EthereumPoW"),
    SaltPay("WXDAI", "WxDAI", "SaltPay"),
    SaltPayTestnet("WXDAI", "WxDAI", "SaltPay Testnet"),
    ;

    fun decimals(): Int = when (this) {
        Unknown -> 0
        Cardano, CardanoShelley,
        XRP,
        Tezos,
        Tron, TronTestnet -> 6
        Stellar, StellarTestnet -> 7
        Bitcoin, BitcoinTestnet,
        BitcoinCash, BitcoinCashTestnet,
        Binance, BinanceTestnet,
        Litecoin,
        Ducatus,
        Dogecoin,
        Dash -> 8
        Solana, SolanaTestnet -> 9
        Polkadot -> 10
        PolkadotTestnet, Kusama -> 12
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
        SaltPay, SaltPayTestnet -> 18
    }

    fun makeAddresses(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray? = null,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): Set<Address> {
        return if (pairPublicKey != null) {
            (getAddressService() as? MultisigAddressProvider)
                ?.makeMultisigAddresses(walletPublicKey, pairPublicKey) ?: emptySet()
        } else {
            getAddressService().makeAddresses(walletPublicKey, curve)
        }
    }

    fun validateAddress(address: String): Boolean = getAddressService().validate(address)

    private fun getAddressService(): AddressService = when (this) {
        Bitcoin, BitcoinTestnet, Litecoin, Dogecoin, Ducatus, Dash -> BitcoinAddressService(this)
        BitcoinCash, BitcoinCashTestnet -> BitcoinCashAddressService()
        Arbitrum, ArbitrumTestnet,
        Ethereum, EthereumTestnet, EthereumClassic, EthereumClassicTestnet,
        BSC, BSCTestnet, Polygon, PolygonTestnet, Avalanche, AvalancheTestnet,
        Fantom, FantomTestnet, Gnosis, Optimism, OptimismTestnet,
        EthereumFair, EthereumPow, EthereumPowTestnet, SaltPay, SaltPayTestnet -> EthereumAddressService()
        RSK -> RskAddressService()
        Cardano, CardanoShelley -> CardanoAddressService(this)
        XRP -> XrpAddressService()
        Binance -> BinanceAddressService()
        BinanceTestnet -> BinanceAddressService(true)
        Polkadot, PolkadotTestnet, Kusama -> PolkadotAddressService(this)
        Stellar, StellarTestnet -> StellarAddressService()
        Solana, SolanaTestnet -> SolanaAddressService()
        Tezos -> TezosAddressService()
        Tron, TronTestnet -> TronAddressService()
        Unknown -> throw Exception("unsupported blockchain")
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

    fun getExploreUrl(address: String, tokenContractAddress: String? = null): String = when (this) {
        Arbitrum -> "https://arbiscan.io/address/$address"
        ArbitrumTestnet -> "https://goerli-rollup-explorer.arbitrum.io/address/$address"
        Avalanche -> "https://snowtrace.io/address/$address"
        AvalancheTestnet -> "https://testnet.snowtrace.io/address/$address"
        Binance -> "https://explorer.binance.org/address/$address"
        BinanceTestnet -> "https://testnet-explorer.binance.org/address/$address"
        Bitcoin -> "https://www.blockchair.com/bitcoin/address/$address"
        BitcoinTestnet -> "https://www.blockchair.com/bitcoin/testnet/address/$address"
        BitcoinCash -> "https://www.blockchair.com/bitcoin-cash/address/$address"
        BitcoinCashTestnet -> "https://www.blockchain.com/bch-testnet/address/$address"
        BSC -> "https://bscscan.com/address/$address"
        BSCTestnet -> "https://testnet.bscscan.com/address/$address"
        Cardano, CardanoShelley -> "https://www.blockchair.com/cardano/address/$address"
        Dogecoin -> "https://blockchair.com/dogecoin/address/$address"
        Ducatus -> "https://insight.ducatus.io/#/DUC/mainnet/address/$address"
        Ethereum -> if (tokenContractAddress == null) {
            "https://etherscan.io/address/$address"
        } else {
            "https://etherscan.io/token/$tokenContractAddress?a=$address"
        }
        EthereumTestnet -> if (tokenContractAddress == null) {
            "https://goerli.etherscan.io/address/$address"
        } else {
            "https://goerli.etherscan.io/token/$tokenContractAddress?a=$address"
        }
        EthereumClassic -> "https://blockscout.com/etc/mainnet/address/$address/transactions"
        EthereumClassicTestnet -> "https://blockscout.com/etc/kotti/address/$address/transactions"
        Fantom -> "https://ftmscan.com/address/$address"
        FantomTestnet -> "https://testnet.ftmscan.com/address/$address"
        Litecoin -> "https://blockchair.com/litecoin/address/$address"
        Polkadot -> "https://polkadot.subscan.io/account/$address"
        PolkadotTestnet -> "https://westend.subscan.io/account/$address"
        Kusama -> "https://kusama.subscan.io/account/$address"
        Polygon -> "https://polygonscan.com/address/$address"
        PolygonTestnet -> "https://explorer-mumbai.maticvigil.com/address/$address"
        RSK -> {
            var url = "https://explorer.rsk.co/address/$address"
            if (tokenContractAddress != null) {
                url += "?__tab=tokens"
            }
            url
        }
        Stellar -> "https://stellar.expert/explorer/public/account/$address"
        StellarTestnet -> "https://stellar.expert/explorer/testnet/account/$address"
        Solana -> "https://explorer.solana.com/address/$address"
        SolanaTestnet -> "https://explorer.solana.com/address/$address/?cluster=devnet"
        Tezos -> "https://tezblock.io/account/$address"
        Tron -> "https://tronscan.org/#/address/$address"
        TronTestnet -> "https://nile.tronscan.org/#/address/$address"
        XRP -> "https://xrpscan.com/account/$address"
        Gnosis -> "https://blockscout.com/xdai/mainnet/address/$address"
        Dash -> "https://blockexplorer.one/dash/mainnet/address/$address"
        Optimism -> "https://optimistic.etherscan.io/address/$address"
        OptimismTestnet -> "https://blockscout.com/optimism/goerli/address/$address"
        EthereumFair -> "https://explorer.etherfair.org/address/$address"
        EthereumPow -> "https://mainnet.ethwscan.com/address/$address"
        EthereumPowTestnet -> "https://iceberg.ethwscan.com/address/$address"
        SaltPay -> "https://blockscout.com/xdai/optimism/address/$address"
        SaltPayTestnet -> "https://blockscout-chiado.gnosistestnet.com/address/$address"
        Unknown -> throw Exception("unsupported blockchain")
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
            SaltPayTestnet -> "https://gnosisfaucet.com"
            else -> null
        }
    }

    fun defaultAddressType(): AddressType = when (this) {
        Bitcoin, BitcoinTestnet, Litecoin -> BitcoinAddressType.Segwit
        CardanoShelley -> CardanoAddressType.Shelley
        else -> DefaultAddressType
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
            SaltPay, SaltPayTestnet -> SaltPayTestnet
            else -> null
        }
    }

    fun getSupportedCurves(): List<EllipticCurve> {
        return when (this) {
            Unknown -> emptyList()
            Tezos,
            XRP -> listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
            Arbitrum, ArbitrumTestnet,
            Bitcoin, BitcoinTestnet,
            BitcoinCash, BitcoinCashTestnet,
            Binance, BinanceTestnet,
            Ethereum, EthereumTestnet,
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
            SaltPay, SaltPayTestnet -> listOf(EllipticCurve.Secp256k1)
            Stellar, StellarTestnet,
            Solana, SolanaTestnet,
            Cardano,
            CardanoShelley,
            Polkadot, PolkadotTestnet, Kusama -> listOf(EllipticCurve.Ed25519)
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
            SaltPay -> Chain.SaltPay.id
            SaltPayTestnet -> Chain.SaltPayTestnet.id
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
                        DerivationNode.Hardened(coinType(style)),
                        DerivationNode.Hardened(0)
                    )
                )
            }
            CardanoShelley -> { //We use shelley for all new cards with HD wallets feature
                //Path according to CIP-1852. https://cips.cardano.org/cips/cip1852/
                DerivationPath(
                    path = listOf(
                        DerivationNode.Hardened(1852),
                        DerivationNode.Hardened(coinType(style)),
                        DerivationNode.Hardened(0),
                        DerivationNode.NonHardened(0),
                        DerivationNode.NonHardened(0)
                    )
                )
            }
            else -> {
                // Standard BIP44
                val bip44 = BIP44(
                    coinType = coinType(style),
                    account = 0,
                    change = BIP44.Chain.External,
                    addressIndex = 0
                )
                bip44.buildPath()
            }
        }
    }

    //    https://github.com/satoshilabs/slips/blob/master/slip-0044.md
    fun coinType(style: DerivationStyle): Long {
        if (isTestnet()) return 1

        val ethCoinType = 60L

        if (style == DerivationStyle.NEW && this.isEvm()) return ethCoinType

        return when (this) {
            Bitcoin, Ducatus -> 0
            Litecoin -> 2
            Dogecoin -> 3
            Dash -> 5
            Ethereum, EthereumPow, EthereumFair -> ethCoinType
            EthereumClassic -> 61
            RSK -> 137
            XRP -> 144
            BitcoinCash -> 145
            Stellar -> 148
            Polkadot -> 354
            Kusama -> 434
            Solana -> 501
            Binance -> 714
            Polygon -> 966
            Fantom -> 1007
            Tezos -> 1729
            Cardano, CardanoShelley -> 1815
            Avalanche -> 9000
            Arbitrum -> 9001
            BSC -> 9006
            Tron -> 195
            Gnosis, SaltPay -> 700
            Optimism -> 614
            else -> throw UnsupportedOperationException()
        }
    }

    fun canHandleTokens(): Boolean = when (this) {
        Arbitrum, ArbitrumTestnet,
        Ethereum, EthereumTestnet,
        BSC, BSCTestnet,
        Binance, BinanceTestnet,
        Polygon, PolygonTestnet,
        Avalanche, AvalancheTestnet,
        Fantom, FantomTestnet,
        EthereumClassic, EthereumClassicTestnet,
        RSK,
        Solana, SolanaTestnet,
        Tron, TronTestnet,
        Gnosis,
        Optimism, OptimismTestnet,
        EthereumFair, EthereumPow, EthereumPowTestnet,
        SaltPay, SaltPayTestnet -> true
        else -> false
    }

    fun isEvm(): Boolean = getChainId() != null

    fun isFeeApproximate(amountType: AmountType): Boolean = when (this) {
        Fantom, FantomTestnet,
        Tron, TronTestnet -> amountType is AmountType.Token
        Arbitrum, ArbitrumTestnet,
        Optimism, OptimismTestnet -> true
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
