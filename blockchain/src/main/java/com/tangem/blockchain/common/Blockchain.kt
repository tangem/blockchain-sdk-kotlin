package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressType
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.*
import com.tangem.commands.common.card.EllipticCurve

enum class Blockchain(
        val id: String,
        val currency: String,
        val fullName: String
) {
    Unknown("", "", ""),
    Bitcoin("BTC", "BTC", "Bitcoin"),
    BitcoinTestnet("BTC/test", "BTCt", "Bitcoin Testnet"),
    BitcoinCash("BCH", "BCH", "Bitcoin Cash"),
    Litecoin("LTC", "LTC", "Litecoin"),
    Dogecoin("DOGE", "DOGE", "Dogecoin"),
    Ducatus("DUC", "DUC", "Ducatus"),
    Ethereum("ETH", "ETH", "Ethereum"),
    EthereumTestnet("ETH/test", "ETHt", "Ethereum Testnet"),
    RSK("RSK", "RBTC", "RSK"),
    Cardano("CARDANO", "ADA", "Cardano"),
    CardanoShelley("CARDANO-S", "ADA", "Cardano"),
    XRP("XRP", "XRP", "XRP Ledger"),
    Binance("BINANCE", "BNB", "Binance"),
    BinanceTestnet("BINANCE/test", "BNBt", "Binance Chain"),
    Stellar("XLM", "XLM", "Stellar"),
    Tezos("XTZ", "XTZ", "Tezos"),
    BSC("BSC", "BNB", "Binance Smart Chain"),
    BSCTestnet("BSC/test", "BNB", "Binance Smart Chain - Testnet"),
    ;

    fun decimals(): Int = when (this) {
        Bitcoin, BitcoinTestnet, BitcoinCash, Binance, BinanceTestnet, Litecoin, Ducatus, Dogecoin -> 8
        Cardano, CardanoShelley, XRP, Tezos -> 6
        Ethereum, EthereumTestnet, RSK, BSC, BSCTestnet -> 18
        Stellar -> 7
        Unknown -> 0
    }

    fun makeAddresses(
            walletPublicKey: ByteArray,
            pairPublicKey: ByteArray? = null,
            curve: EllipticCurve = EllipticCurve.Secp256k1
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
        BitcoinCash -> BitcoinCashAddressService()
        Ethereum, EthereumTestnet -> EthereumAddressService()
        RSK -> RskAddressService()
        Cardano, CardanoShelley -> CardanoAddressService(this)
        XRP -> XrpAddressService()
        Binance -> BinanceAddressService()
        BinanceTestnet -> BinanceAddressService(true)
        Stellar -> StellarAddressService()
        Tezos -> TezosAddressService()
        BSC, BSCTestnet -> EthereumAddressService()
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
        Binance -> "https://explorer.binance.org/address/$address"
        BinanceTestnet -> "https://testnet-explorer.binance.org/address/$address"
        Bitcoin -> "https://www.blockchain.com/btc/address/$address"
        BitcoinTestnet -> "https://live.blockcypher.com/btc-testnet/address/$address"
        BitcoinCash -> "https://blockchair.com/bitcoin-cash/address/$address"
        Litecoin -> "https://live.blockcypher.com/ltc/address/$address"
        Dogecoin -> "https://blockchair.com/dogecoin/address/$address"
        Ducatus -> "https://insight.ducatus.io/#/DUC/mainnet/address/$address"
        Cardano, CardanoShelley -> "https://cardanoexplorer.com/address/$address"
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
        RSK -> {
            var url = "https://explorer.rsk.co/address/$address"
            if (tokenContractAddress != null) {
                url += "?__tab=tokens"
            }
            url
        }
        Stellar -> "https://stellar.expert/explorer/public/account/$address"
        XRP -> "https://xrpscan.com/account/$address"
        Tezos -> "https://tezblock.io/account/$address"
        BSC -> "https://bscscan.com/address/$address"
        BSCTestnet -> "https://testnet.bscscan.com/address/$address"
        Unknown -> throw Exception("unsupported blockchain")
    }

    fun defaultAddressType(): AddressType = when (this) {
        Bitcoin, BitcoinTestnet -> BitcoinAddressType.Segwit
        CardanoShelley -> CardanoAddressType.Shelley
        else -> DefaultAddressType
    }

    fun tokenDisplayName(): String = when (this) { // TODO: do we need it?
        Ethereum -> "Ethereum smart contract token"
        Stellar -> "Stellar Asset"
        Binance -> "Binance Asset"
        else -> fullName
    }

    companion object {
        private val values = values()
        fun fromId(id: String): Blockchain = values.find { it.id == id } ?: Unknown
        fun fromName(name: String): Blockchain = values.find { it.name == name } ?: Unknown
        fun fromCurrency(currency: String): Blockchain = values.find { it.currency == currency }
                ?: Unknown
    }
}