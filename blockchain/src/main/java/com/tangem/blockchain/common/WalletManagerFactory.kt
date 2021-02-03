package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkManager
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashNetworkManager
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkManager
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkManager
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarNetworkManager
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkManager
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.commands.common.card.Card

object WalletManagerFactory {

    fun makeWalletManager(card: Card, tokens: Set<Token>? = null): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null
        val blockchain = Blockchain.fromId(blockchainName)

        val cardId = card.cardId
        val addresses = blockchain.makeAddresses(walletPublicKey)
        val presetTokens = tokens ?: getToken(card)?.let { setOf(it) } ?: emptySet()

        val wallet = Wallet(blockchain, addresses, presetTokens)

        return when (blockchain) {
            Blockchain.Bitcoin -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        BitcoinNetworkManager(blockchain)
                )
            }
            Blockchain.BitcoinTestnet -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        BitcoinNetworkManager(blockchain)
                )
            }
            Blockchain.BitcoinCash -> {
                BitcoinCashWalletManager(
                        cardId, wallet,
                        BitcoinCashTransactionBuilder(walletPublicKey, blockchain),
                        BitcoinCashNetworkManager()
                )
            }
            Blockchain.Litecoin -> {
                LitecoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        LitecoinNetworkManager()
                )
            }
            Blockchain.Ducatus -> {
                DucatusWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        DucatusNetworkManager()
                )
            }
            Blockchain.Ethereum, Blockchain.EthereumTestnet, Blockchain.RSK -> {
                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkManager(blockchain),
                        presetTokens
                )
            }
            Blockchain.Stellar -> {
                val networkService = StellarNetworkManager()

                StellarWalletManager(
                        cardId, wallet,
                        StellarTransactionBuilder(networkService, walletPublicKey),
                        networkService,
                        presetTokens
                )
            }
            Blockchain.Cardano, Blockchain.CardanoShelley -> {
                CardanoWalletManager(
                        cardId, wallet,
                        CardanoTransactionBuilder(walletPublicKey),
                        CardanoNetworkManager()
                )
            }
            Blockchain.XRP -> {
                val networkService = XrpNetworkManager()

                XrpWalletManager(
                        cardId, wallet,
                        XrpTransactionBuilder(networkService, walletPublicKey),
                        networkService
                )
            }
            Blockchain.Binance -> {
                BinanceWalletManager(
                        cardId, wallet,
                        BinanceTransactionBuilder(walletPublicKey),
                        BinanceNetworkManager(),
                        presetTokens
                )
            }
            Blockchain.BinanceTestnet -> {
                BinanceWalletManager(
                        cardId, wallet,
                        BinanceTransactionBuilder(walletPublicKey, true),
                        BinanceNetworkManager(true),
                        presetTokens
                )
            }
            Blockchain.Tezos -> {
                TezosWalletManager(
                        cardId, wallet,
                        TezosTransactionBuilder(walletPublicKey),
                        TezosNetworkManager()
                )
            }
            Blockchain.Unknown -> throw Exception("unsupported blockchain")
        }
    }


    fun makeMultisigWalletManager(
            card: Card, pairPublicKey: ByteArray, tokens: Set<Token>? = null
    ): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null
        val blockchain = Blockchain.fromId(blockchainName)

        val cardId = card.cardId
        val addresses = blockchain.makeMultisigAddresses(walletPublicKey, pairPublicKey)
                ?: return null
        val presetTokens = tokens ?: getToken(card)?.let { setOf(it) } ?: emptySet()

        val wallet = Wallet(blockchain, addresses, presetTokens)

        return when (blockchain) {
            Blockchain.Bitcoin -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        BitcoinNetworkManager(blockchain)
                )
            }
            Blockchain.BitcoinTestnet -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        BitcoinNetworkManager(blockchain)
                )
            }
            else -> return null
        }
    }

    private fun getToken(card: Card): Token? {
        val symbol = card.cardData?.tokenSymbol ?: return null
        val contractAddress = card.cardData?.tokenContractAddress ?: return null
        val decimals = card.cardData?.tokenDecimal ?: return null
        return Token(symbol, contractAddress, decimals)
    }
}