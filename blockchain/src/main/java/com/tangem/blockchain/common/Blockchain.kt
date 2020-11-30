package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.address.DefaultAddressType

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
    Ducatus("DUC", "DUC", "Ducatus"),
    Ethereum("ETH", "ETH", "Ethereum"),
    EthereumTestnet("ETH/test", "ETH", "Ethereum Testnet"),
    RSK("RSK", "RBTC", "RSK"),
    Cardano("CARDANO", "ADA", "Cardano"),
    CardanoShelley("CARDANO-S", "ADA", "Cardano"),
    XRP("XRP", "XRP", "XRP Ledger"),
    Binance("BINANCE", "BNB", "Binance"),
    BinanceTestnet("BINANCE/test", "BNBt", "Binance Chain"),
    Stellar("XLM", "XLM", "Stellar"),
    Tezos("XTZ", "XTZ", "Tezos");

    fun decimals(): Int = when (this) {
        Bitcoin, BitcoinTestnet, BitcoinCash, Binance, BinanceTestnet, Litecoin, Ducatus -> 8
        Cardano, CardanoShelley, XRP, Tezos -> 6
        Ethereum, EthereumTestnet, RSK -> 18
        Stellar -> 7
        Unknown -> 0
    }

//    fun makeAddress(walletPublicKey: ByteArray): String = //TODO: shall we leave it for backwards compatibility?
//            getAddressService().makeAddress(walletPublicKey)

    fun makeAddresses(walletPublicKey: ByteArray): Set<Address> =
            getAddressService().makeAddresses(walletPublicKey)

    fun validateAddress(address: String): Boolean = getAddressService().validate(address)

    private fun getAddressService(): AddressService = when (this) {
        Bitcoin, BitcoinTestnet, Litecoin, Ducatus -> BitcoinAddressService(this)
        BitcoinCash -> BitcoinCashAddressService()
        Ethereum, EthereumTestnet -> EthereumAddressService()
        RSK -> RskAddressService()
        Cardano, CardanoShelley -> CardanoAddressService(this)
        XRP -> XrpAddressService()
        Binance -> BinanceAddressService()
        BinanceTestnet -> BinanceAddressService(true)
        Stellar -> StellarAddressService()
        Tezos -> TezosAddressService()
        Unknown -> throw Exception("unsupported blockchain")
    }

    fun getShareUri(address: String): String = when (this) {
        Bitcoin -> "bitcoin:$address"
        Ethereum -> "ethereum:$address"
        XRP -> "xrpl:$address"
        Litecoin -> "litecoin:$address"
        Binance -> "bnb:$address"
        else -> address
    }

    fun getExploreUrl(address: String, tokenContractAddress: String? = null): String = when (this) {
        Binance -> "https://explorer.binance.org/address/$address"
        BinanceTestnet -> "https://testnet-explorer.binance.org/address/$address"
        Bitcoin -> "https://blockchain.info/address/$address"
        BitcoinTestnet -> "https://live.blockcypher.com/btc-testnet/address/$address"
        BitcoinCash -> "https://blockchair.com/bitcoin-cash/address/$address"
        Litecoin -> "https://live.blockcypher.com/ltc/address/$address"
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
        Unknown -> throw Exception("unsupported blockchain")
    }

    fun defaultAddressType(): AddressType = when (this) {
        Bitcoin, BitcoinTestnet -> BitcoinAddressType.Segwit
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
