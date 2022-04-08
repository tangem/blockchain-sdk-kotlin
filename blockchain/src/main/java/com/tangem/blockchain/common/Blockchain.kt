package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressType
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
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
    Avalanche("AVALANCHE", "AVAX", "Avalanche C-Chain"),
    AvalancheTestnet("AVALANCHE/test", "AVAX", "Avalanche C-Chain Testnet"),
    Binance("BINANCE", "BNB", "Binance"),
    BinanceTestnet("BINANCE/test", "BNB", "Binance Testnet"),
    BSC("BSC", "BNB", "Binance Smart Chain"),
    BSCTestnet("BSC/test", "BNB", "Binance Smart Chain Testnet"),
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
    Fantom("FTM", "FTM", "Fantom"),
    FantomTestnet("FTM/test", "FTM", "Fantom Testnet"),
    Litecoin("LTC", "LTC", "Litecoin"),
    Polygon("POLYGON", "MATIC", "Polygon"),
    PolygonTestnet("POLYGON/test", "MATIC", "Polygon Testnet"),
    RSK("RSK", "RBTC", "RSK"),
    Stellar("XLM", "XLM", "Stellar"),
    StellarTestnet("XLM/test", "XLM", "Stellar Testnet"),
    Solana("SOLANA", "SOL", "Solana"),
    SolanaTestnet("SOLANA/test", "SOL", "Solana Testnet"),
    Tezos("XTZ", "XTZ", "Tezos"),
    XRP("XRP", "XRP", "XRP Ledger"),
    ;

    fun decimals(): Int = when (this) {
        Bitcoin, BitcoinTestnet, BitcoinCash, BitcoinCashTestnet,
        Binance, BinanceTestnet, Litecoin, Ducatus, Dogecoin,
        -> 8
        Cardano, CardanoShelley, XRP, Tezos -> 6
        Ethereum, EthereumTestnet, RSK, BSC, BSCTestnet, Polygon, PolygonTestnet, Avalanche, AvalancheTestnet,
        Fantom, FantomTestnet -> 18
        Stellar, StellarTestnet -> 7
        Solana, SolanaTestnet -> 9
        Unknown -> 0
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
        Bitcoin, BitcoinTestnet, Litecoin, Dogecoin, Ducatus -> BitcoinAddressService(this)
        BitcoinCash, BitcoinCashTestnet -> BitcoinCashAddressService()
        Ethereum, EthereumTestnet, BSC, BSCTestnet, Polygon, PolygonTestnet, Avalanche, AvalancheTestnet,
        Fantom, FantomTestnet -> EthereumAddressService()
        RSK -> RskAddressService()
        Cardano, CardanoShelley -> CardanoAddressService(this)
        XRP -> XrpAddressService()
        Binance -> BinanceAddressService()
        BinanceTestnet -> BinanceAddressService(true)
        Stellar, StellarTestnet -> StellarAddressService()
        Solana, SolanaTestnet -> SolanaAddressService()
        Tezos -> TezosAddressService()
        Unknown -> throw Exception("unsupported blockchain")
    }

    fun getShareScheme(): String? = when (this) {
        Bitcoin -> "bitcoin"
        Ethereum -> "ethereum"
        Litecoin -> "litecoin"
        Binance -> "bnb"
        else -> null
    }

    fun getShareUri(address: String): String = getShareScheme()?.plus(":$address")
        ?: address

    fun validateShareScheme(scheme: String): Boolean {
        if (this == XRP && (scheme == "ripple" || scheme == "xrpl" || scheme == "xrp")) return true
        return scheme == getShareScheme()
    }

    fun getExploreUrl(address: String, tokenContractAddress: String? = null): String = when (this) {
        Avalanche -> "https://snowtrace.io/address/$address"
        AvalancheTestnet -> "https://testnet.snowtrace.io/address/$address"
        Binance -> "https://explorer.binance.org/address/$address"
        BinanceTestnet -> "https://testnet-explorer.binance.org/address/$address"
        Bitcoin -> "https://www.blockchain.com/btc/address/$address"
        BitcoinTestnet -> "https://www.blockchain.com/btc-testnet/address/$address"
        BitcoinCash -> "https://www.blockchain.com/bch/address/$address"
        BitcoinCashTestnet -> "https://www.blockchain.com/bch-testnet/address/$address"
        Litecoin -> "https://blockchair.com/litecoin/address/$address"
        Dogecoin -> "https://blockchair.com/dogecoin/address/$address"
        Ducatus -> "https://insight.ducatus.io/#/DUC/mainnet/address/$address"
        Cardano, CardanoShelley -> "https://explorer.cardano.org/en/address.html?address=$address"
        Ethereum -> if (tokenContractAddress == null) {
            "https://etherscan.io/address/$address"
        } else {
            "https://etherscan.io/token/$tokenContractAddress?a=$address"
        }
        EthereumTestnet -> if (tokenContractAddress == null) {
            "https://rinkeby.etherscan.io/address/$address"
        } else {
            "https://rinkeby.etherscan.io/token/$tokenContractAddress?a=$address"
        }
        Fantom -> "https://ftmscan.com/address/$address"
        FantomTestnet -> "https://testnet.ftmscan.com/address/$address"
        RSK -> {
            var url = "https://explorer.rsk.co/address/$address"
            if (tokenContractAddress != null) {
                url += "?__tab=tokens"
            }
            url
        }
        BSC -> "https://bscscan.com/address/$address"
        BSCTestnet -> "https://testnet.bscscan.com/address/$address"
        Polygon -> "https://polygonscan.com/address/$address"
        PolygonTestnet -> "https://explorer-mumbai.maticvigil.com/address/$address"
        Stellar -> "https://stellar.expert/explorer/public/account/$address"
        StellarTestnet -> "https://stellar.expert/explorer/testnet/account/$address"
        Solana -> "https://explorer.solana.com/address/$address"
        SolanaTestnet -> "https://explorer.solana.com/address/$address/?cluster=devnet"
        XRP -> "https://xrpscan.com/account/$address"
        Tezos -> "https://tezblock.io/account/$address"
        Unknown -> throw Exception("unsupported blockchain")
    }

    fun getTestnetTopUpUrl(): String? {
        return when (this) {
            AvalancheTestnet -> "https://faucet.avax-test.network/"
            BitcoinTestnet -> "https://coinfaucet.eu/en/btc-testnet/"
            EthereumTestnet -> "https://faucet.rinkeby.io"
            BitcoinCashTestnet -> "https://coinfaucet.eu/en/bch-testnet/"
            BinanceTestnet -> "https://docs.binance.org/smart-chain/wallet/binance.html"
            BSCTestnet -> "https://testnet.binance.org/faucet-smart"
            FantomTestnet -> "https://faucet.fantom.network"
            PolygonTestnet -> "https://faucet.matic.network"
            StellarTestnet -> "https://laboratory.stellar.org/#account-creator?network=test"
            SolanaTestnet -> "https://solfaucet.com/"
            else -> null
        }
    }

    fun defaultAddressType(): AddressType = when (this) {
        Bitcoin, BitcoinTestnet -> BitcoinAddressType.Segwit //TODO: add Litecoin when sending from Segwit is fixed
        CardanoShelley -> CardanoAddressType.Shelley
        else -> DefaultAddressType
    }

    fun tokenDisplayName(): String = when (this) {
        Ethereum, EthereumTestnet -> "Ethereum smart contract token"
        Stellar, StellarTestnet -> "Stellar Asset"
        Binance, BinanceTestnet -> "Binance Asset"
        BSC, BSCTestnet -> "Binance Smart Chain Token"
        Solana, SolanaTestnet -> "Solana Token"
        else -> fullName
    }

    fun isTestnet(): Boolean {
        return when (this) {
            Unknown, Avalanche, Bitcoin, BitcoinCash, Litecoin, Dogecoin, Ducatus, Ethereum, RSK, BSC, Polygon,
            Cardano, CardanoShelley, XRP, Binance, Stellar, Solana, Tezos, Fantom
            -> false
            AvalancheTestnet, BitcoinTestnet, EthereumTestnet, BSCTestnet, PolygonTestnet, BinanceTestnet,
            BitcoinCashTestnet, StellarTestnet, SolanaTestnet, FantomTestnet
            -> true
        }
    }

    fun getTestnetVersion(): Blockchain? {
        return when (this) {
            Avalanche, AvalancheTestnet -> AvalancheTestnet
            Bitcoin, BitcoinTestnet -> BitcoinTestnet
            BitcoinCash, BitcoinCashTestnet -> BitcoinCashTestnet
            Ethereum, EthereumTestnet -> EthereumTestnet
            Binance, BinanceTestnet -> BinanceTestnet
            BSC, BSCTestnet -> BSCTestnet
            Fantom, FantomTestnet -> FantomTestnet
            Polygon, PolygonTestnet -> PolygonTestnet
            Stellar, StellarTestnet -> StellarTestnet
            Solana, SolanaTestnet -> SolanaTestnet
            Litecoin -> null
            Dogecoin -> null
            Ducatus -> null
            RSK -> null
            Cardano -> null
            CardanoShelley -> null
            XRP -> null
            Tezos -> null
            Unknown -> null
        }
    }

    fun getSupportedCurves(): List<EllipticCurve> {
        return when (this) {
            Unknown -> emptyList()
            Bitcoin, BitcoinTestnet, BitcoinCash, BitcoinCashTestnet, Litecoin, Ducatus,
            Ethereum, EthereumTestnet, RSK, Binance, BinanceTestnet, Dogecoin, BSC, BSCTestnet,
            Polygon, PolygonTestnet, Avalanche, AvalancheTestnet, Fantom, FantomTestnet
            -> listOf(EllipticCurve.Secp256k1)
            Tezos, XRP -> listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
            Cardano, CardanoShelley, Stellar, StellarTestnet, Solana, SolanaTestnet ->
                listOf(EllipticCurve.Ed25519)
        }
    }

    fun getChainId(): Int? {
        return when (this) {
            Avalanche -> Chain.Avalanche.id
            AvalancheTestnet -> Chain.AvalancheTestnet.id
            Ethereum -> Chain.Mainnet.id
            EthereumTestnet -> Chain.Rinkeby.id
            Fantom -> Chain.Fantom.id
            FantomTestnet -> Chain.FantomTestnet.id
            RSK -> Chain.RskMainnet.id
            BSC -> Chain.BscMainnet.id
            BSCTestnet -> Chain.BscTestnet.id
            Polygon -> Chain.Polygon.id
            PolygonTestnet -> Chain.PolygonTestnet.id
            else -> null
        }
    }

    fun derivationPath(style: DerivationStyle): DerivationPath? {
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
            Ethereum -> ethCoinType
            RSK -> 137
            XRP -> 144
            BitcoinCash -> 145
            Stellar -> 148
            Solana -> 501
            Binance -> 714
            Polygon -> 966
            Fantom -> 1007
            Tezos -> 1729
            Cardano, CardanoShelley -> 1815
            Avalanche -> 9000
            BSC -> 9006
            else -> throw UnsupportedOperationException()
        }
    }

    companion object {
        private val values = values()
        fun fromId(id: String): Blockchain = values.find { it.id == id } ?: Unknown

        fun fromChainId(chainId: Int): Blockchain? {
            return when (chainId) {
                Chain.Avalanche.id -> Avalanche
                Chain.AvalancheTestnet.id -> AvalancheTestnet
                Chain.Mainnet.id -> Ethereum
                Chain.Rinkeby.id -> EthereumTestnet
                Chain.RskMainnet.id -> RSK
                Chain.BscMainnet.id -> BSC
                Chain.BscTestnet.id -> BSCTestnet
                Chain.Polygon.id -> Polygon
                Chain.PolygonTestnet.id -> PolygonTestnet
                Chain.Fantom.id -> Fantom
                Chain.FantomTestnet.id -> FantomTestnet
                else -> null
            }
        }


        fun secp256k1Blockchains(isTestnet: Boolean): List<Blockchain> {
            return if (isTestnet) {
                listOf(
                    AvalancheTestnet,
                    BitcoinTestnet,
                    BitcoinCashTestnet,
                    BinanceTestnet,
                    BSCTestnet,
                    EthereumTestnet,
                    PolygonTestnet,
                    FantomTestnet,
                )
            } else {
                listOf(
                    Avalanche,
                    Bitcoin,
                    BitcoinCash,
                    Binance,
                    BSC,
                    Litecoin,
                    XRP,
                    Tezos,
                    Ethereum,
                    RSK,
                    Polygon,
                    Dogecoin,
                    Fantom,
                )
            }
        }

        fun ed25519OnlyBlockchains(isTestnet: Boolean): List<Blockchain> {
            return if (isTestnet) {
                listOf(StellarTestnet, SolanaTestnet)
            } else {
                listOf(CardanoShelley, Stellar, Solana)
            }
        }
    }

    fun canHandleTokens(): Boolean {
        return when (this) {
            Ethereum, EthereumTestnet,
            BSC, BSCTestnet,
            Binance, BinanceTestnet,
            Polygon, PolygonTestnet,
            Avalanche, AvalancheTestnet,
            Solana, SolanaTestnet,
            Fantom, FantomTestnet -> true
            else -> false
        }
    }

    fun isEvm(): Boolean {
        return getChainId() != null
    }


}