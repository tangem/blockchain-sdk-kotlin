package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkManager
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashNetworkManager
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteProvider
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkManager
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarNetworkManager
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkManager
import com.tangem.blockchain.blockchains.tezos.network.TezosProvider
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledProvider
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockchair.BlockchairEthProvider
import com.tangem.blockchain.network.blockchair.BlockchairProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherProvider
import com.tangem.commands.common.card.Card

class WalletManagerFactory(
        private val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig()
) {

    fun makeWalletManager(card: Card, tokens: Set<Token>? = null): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchainName: String = card.cardData?.blockchainName ?: return null
        val blockchain = Blockchain.fromId(blockchainName)

        val cardId = card.cardId
        val addresses = blockchain.makeAddresses(walletPublicKey)
        val presetTokens = tokens ?: getToken(card)?.let { setOf(it) } ?: emptySet()

        val wallet = Wallet(blockchain, addresses, presetTokens)

        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkManager(blockchain)
                )
            }

            Blockchain.Litecoin -> {
                LitecoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkManager(blockchain)
                )
            }

            Blockchain.BitcoinCash -> {
                BitcoinCashWalletManager(
                        cardId, wallet,
                        BitcoinCashTransactionBuilder(walletPublicKey, blockchain),
                        BitcoinCashNetworkManager(blockchainSdkConfig.blockchairApiKey)
                )
            }
            Blockchain.Ducatus -> {
                DucatusWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        DucatusNetworkManager()
                )
            }
            Blockchain.Ethereum -> {
                val blockchairProvider by lazy {
                    BlockchairEthProvider(blockchainSdkConfig.blockchairApiKey)
                }
                val blockcypherProvider by lazy {
                    BlockcypherProvider(blockchain, blockchainSdkConfig.blockcypherTokens)
                }
                val networkManager = EthereumNetworkManager(
                        blockchain,
                        blockchainSdkConfig.infuraProjectId,
                        blockcypherProvider,
                        blockchairProvider
                )

                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        networkManager,
                        presetTokens
                )
            }
            Blockchain.EthereumTestnet, Blockchain.RSK -> {
                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkManager(blockchain, blockchainSdkConfig.infuraProjectId),
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
                val adaliteProvider1 by lazy { AdaliteProvider(API_ADALITE) }
                val adaliteProvider2 by lazy { AdaliteProvider(API_ADALITE_RESERVE) }
                val providers = listOf(adaliteProvider1, adaliteProvider2)

                CardanoWalletManager(
                        cardId, wallet,
                        CardanoTransactionBuilder(walletPublicKey),
                        CardanoNetworkManager(providers)
                )
            }
            Blockchain.XRP -> {
                val rippledProvider1 by lazy { RippledProvider(API_RIPPLED) }
                val rippledProvider2 by lazy { RippledProvider(API_RIPPLED_RESERVE) }
                val providers = listOf(rippledProvider1, rippledProvider2)
                val networkService = XrpNetworkManager(providers)

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
                val tezosProvider1 by lazy { TezosProvider(API_TEZOS) }
                val tezosProvider2 by lazy { TezosProvider(API_TEZOS_RESERVE) }
                val providers = listOf(tezosProvider1, tezosProvider2)

                TezosWalletManager(
                        cardId, wallet,
                        TezosTransactionBuilder(walletPublicKey),
                        TezosNetworkManager(providers)
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
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkManager(blockchain)
                )
            }
            else -> return null
        }
    }

    private fun makeBitcoinNetworkManager(blockchain: Blockchain): BitcoinNetworkManager {
        val providers = mutableListOf<BitcoinNetworkService>()

        val blockchairProvider by lazy {
            BlockchairProvider(blockchain, blockchainSdkConfig.blockchairApiKey)
        }
        providers.add(blockchairProvider)

        val blockcypherTokens = blockchainSdkConfig.blockcypherTokens
        if (!blockcypherTokens.isNullOrEmpty()) {
            val blockcypherProvider by lazy {
                BlockcypherProvider(blockchain, blockcypherTokens)
            }
            providers.add(blockcypherProvider)
        }
        return BitcoinNetworkManager(providers)
    }

    private fun getToken(card: Card): Token? {
        val symbol = card.cardData?.tokenSymbol ?: return null
        val contractAddress = card.cardData?.tokenContractAddress ?: return null
        val decimals = card.cardData?.tokenDecimal ?: return null
        return Token(symbol, contractAddress, decimals)
    }
}