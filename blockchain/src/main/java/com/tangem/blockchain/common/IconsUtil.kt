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
            if (!blockchain.tokenIconsAvailable()) return null
            val blockchainPath = blockchain.getPath() ?: return null
            val tokenPath = normalizeAssetPath(token.contractAddress, blockchain)
            return Uri.parse("$BASE_URL/$blockchainPath/assets/$tokenPath/logo.png")
        }

        private fun Blockchain.getPath(): String? = when (this) {
            Blockchain.Bitcoin -> "bitcoin"
            Blockchain.BitcoinCash -> "bitcoincash"
            Blockchain.Litecoin -> "litecoin"
            Blockchain.Dogecoin -> "doge"
            Blockchain.Ethereum -> "ethereum"
            Blockchain.Cardano, Blockchain.CardanoShelley -> "cardano"
            Blockchain.XRP -> "xrp"
            Blockchain.Binance -> "binance"
            Blockchain.Stellar -> "stellar"
            Blockchain.Tezos -> "tezos"
            else -> null
        }

        private fun Blockchain.tokenIconsAvailable(): Boolean = when (this) {
            Blockchain.Ethereum, Blockchain.Binance -> true
            else -> false
        }

        private fun normalizeAssetPath(assetPath: String, blockchain: Blockchain): String =
                when (blockchain) {
                    Blockchain.Ethereum -> Address(assetPath).withERC55Checksum().hex
                    Blockchain.Binance -> assetPath.toUpperCase(Locale.ROOT)
                    else -> assetPath
                }
    }
}