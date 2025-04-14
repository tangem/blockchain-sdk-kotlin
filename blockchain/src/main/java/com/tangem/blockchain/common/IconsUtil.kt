package com.tangem.blockchain.common

import android.net.Uri
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.model.Address
import java.util.Locale

object IconsUtil {
    private const val BASE_URL = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains"

    fun getBlockchainIconUri(blockchain: Blockchain): Uri? {
        val blockchainPath = blockchain.getPath() ?: return null

        return Uri.parse("$BASE_URL/$blockchainPath/info/logo.png")
    }

    fun getTokenIconUri(blockchain: Blockchain, token: Token): Uri? {
        val blockchainPath = blockchain.getPath() ?: return null

        val tokenPath = normalizeAssetPath(token, blockchain)
        return Uri.parse("$BASE_URL/$blockchainPath/assets/$tokenPath/logo.png")
    }

    @Suppress("CyclomaticComplexMethod")
    private fun Blockchain.getPath(): String? = when (this) {
        Blockchain.Avalanche, Blockchain.AvalancheTestnet -> "avalanchec"
        Blockchain.Binance, Blockchain.BinanceTestnet -> "binance"
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> "bitcoin"
        Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> "bitcoincash"
        Blockchain.BSC, Blockchain.BSCTestnet -> "smartchain"
        Blockchain.Cardano -> "cardano"
        Blockchain.Dogecoin -> "doge"
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ethereum"
        Blockchain.Fantom, Blockchain.FantomTestnet -> "fantom"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.Polkadot, Blockchain.PolkadotTestnet -> "polkadot"
        Blockchain.Kusama -> "kusama"
        Blockchain.Polygon, Blockchain.PolygonTestnet -> "polygon"
        Blockchain.Solana, Blockchain.SolanaTestnet -> "solana"
        Blockchain.Stellar, Blockchain.StellarTestnet -> "stellar"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "xrp"
        else -> null
    }

    private fun normalizeAssetPath(token: Token, blockchain: Blockchain): String {
        val path = token.contractAddress

        return when (blockchain) {
            Blockchain.Binance -> path.uppercase(Locale.ROOT)
            Blockchain.Ethereum -> Address(path).withERC55Checksum().hex
            else -> path
        }
    }
}
