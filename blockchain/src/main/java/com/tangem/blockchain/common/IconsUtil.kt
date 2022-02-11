package com.tangem.blockchain.common

import android.net.Uri
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.model.Address
import java.util.*

class IconsUtil {
    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains"

        fun getBlockchainIconUri(blockchain: Blockchain): Uri? {
            val blockchainPath = blockchain.getPath() ?: return null

            return Uri.parse("$BASE_URL/$blockchainPath/info/logo.png")
        }

        fun getTokenIconUri(blockchain: Blockchain, token: Token): Uri? {
            val blockchainPath = blockchain.getPath() ?: return null

            val tokenPath = normalizeAssetPath(token)
            return Uri.parse("$BASE_URL/$blockchainPath/assets/$tokenPath/logo.png")
        }

        private fun Blockchain.getPath(): String? = when (this) {
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> "avalanchec"
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> "bitcoin"
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> "bitcoincash"
            Blockchain.Litecoin -> "litecoin"
            Blockchain.Dogecoin -> "doge"
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ethereum"
            Blockchain.BSC, Blockchain.BSCTestnet -> "smartchain"
            Blockchain.Polygon, Blockchain.PolygonTestnet -> "polygon"
            Blockchain.Fantom, Blockchain.FantomTestnet -> "fantom"
            Blockchain.Cardano, Blockchain.CardanoShelley -> "cardano"
            Blockchain.XRP -> "xrp"
            Blockchain.Binance, Blockchain.BinanceTestnet -> "binance"
            Blockchain.Stellar, Blockchain.StellarTestnet -> "stellar"
            Blockchain.Solana, Blockchain.SolanaTestnet -> "solana"
            Blockchain.Tezos -> "tezos"
            else -> null
        }

        private fun normalizeAssetPath(token: Token): String {
            val path = token.contractAddress

            return when (token.blockchain) {
                Blockchain.Ethereum -> Address(path).withERC55Checksum().hex
                Blockchain.Binance -> path.toUpperCase(Locale.ROOT)
                else -> path
            }
        }

    }
}