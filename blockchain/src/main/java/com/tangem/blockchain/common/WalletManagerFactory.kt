package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteProvider
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkService
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledProvider
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.commands.common.card.Card

class WalletManagerFactory(
        private val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig()
) {

    fun makeWalletManager(card: Card,  tokens: Set<Token>? = null): WalletManager? {
        val blockchainName: String = card.cardData?.blockchainName ?: return null
        val blockchain = Blockchain.fromId(blockchainName)
        return makeWalletManager(card, blockchain, tokens)
    }

    fun makeWalletManager(card: Card, blockchain: Blockchain, tokens: Set<Token>? = null): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null

        val cardId = card.cardId
        val addresses = blockchain.makeAddresses(walletPublicKey)
        val presetTokens = getToken(card)?.let { mutableSetOf(it) }
        val canManageTokens: Boolean

        val tokensForWallet = if (presetTokens != null) {
            canManageTokens = false
            presetTokens
        } else {
            canManageTokens = true
            tokens?.toMutableSet() ?: mutableSetOf()
        }

        val wallet = Wallet(blockchain, addresses, tokensForWallet)

        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                BitcoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkService(blockchain)
                )
            }

            Blockchain.Litecoin -> {
                LitecoinWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkService(blockchain)
                )
            }

            Blockchain.BitcoinCash -> {
                BitcoinCashWalletManager(
                        cardId, wallet,
                        BitcoinCashTransactionBuilder(walletPublicKey, blockchain),
                        BitcoinCashNetworkService(blockchainSdkConfig.blockchairApiKey)
                )
            }
            Blockchain.Ducatus -> {
                DucatusWalletManager(
                        cardId, wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain),
                        DucatusNetworkService()
                )
            }
            Blockchain.Ethereum -> {
                val blockchairEthNetworkProvider by lazy {
                    BlockchairEthNetworkProvider(blockchainSdkConfig.blockchairApiKey)
                }
                val blockcypherNetworkProvider by lazy {
                    BlockcypherNetworkProvider(blockchain, blockchainSdkConfig.blockcypherTokens)
                }
                val networkService = EthereumNetworkService(
                        blockchain,
                        blockchainSdkConfig.infuraProjectId,
                        blockcypherNetworkProvider,
                        blockchairEthNetworkProvider
                )

                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        networkService,
                        tokensForWallet, canManageTokens
                )
            }
            Blockchain.EthereumTestnet -> {
                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkService(blockchain, blockchainSdkConfig.infuraProjectId),
                        tokensForWallet, canManageTokens
                )
            }
            Blockchain.RSK -> {
                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkService(blockchain, blockchainSdkConfig.infuraProjectId),
                        tokensForWallet
                )
            }
            Blockchain.Stellar -> {
                val networkService = StellarNetworkService()

                StellarWalletManager(
                        cardId, wallet,
                        StellarTransactionBuilder(networkService, walletPublicKey),
                        networkService,
                        tokensForWallet
                )
            }
            Blockchain.Cardano, Blockchain.CardanoShelley -> {
                val adaliteNetworkProvider1 by lazy { AdaliteProvider(API_ADALITE) }
                val adaliteNetworkProvider2 by lazy { AdaliteProvider(API_ADALITE_RESERVE) }
                val providers = listOf(adaliteNetworkProvider1, adaliteNetworkProvider2)

                CardanoWalletManager(
                        cardId, wallet,
                        CardanoTransactionBuilder(walletPublicKey),
                        CardanoNetworkService(providers)
                )
            }
            Blockchain.XRP -> {
                val rippledProvider1 by lazy { RippledProvider(API_RIPPLED) }
                val rippledProvider2 by lazy { RippledProvider(API_RIPPLED_RESERVE) }
                val providers = listOf(rippledProvider1, rippledProvider2)
                val networkService = XrpNetworkService(providers)

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
                        BinanceNetworkService(),
                        tokensForWallet
                )
            }
            Blockchain.BinanceTestnet -> {
                BinanceWalletManager(
                        cardId, wallet,
                        BinanceTransactionBuilder(walletPublicKey, true),
                        BinanceNetworkService(true),
                        tokensForWallet
                )
            }
            Blockchain.Tezos -> {
                val tezosJsonRpcProvider1 by lazy { TezosJsonRpcProvider(API_TEZOS) }
                val tezosJsonRpcProvider2 by lazy { TezosJsonRpcProvider(API_TEZOS_RESERVE) }
                val providers = listOf(tezosJsonRpcProvider1, tezosJsonRpcProvider2)

                TezosWalletManager(
                        cardId, wallet,
                        TezosTransactionBuilder(walletPublicKey),
                        TezosNetworkService(providers)
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
                        makeBitcoinNetworkService(blockchain)
                )
            }
            else -> return null
        }
    }

    private fun makeBitcoinNetworkService(blockchain: Blockchain): BitcoinNetworkService {
        val providers = mutableListOf<BitcoinNetworkProvider>()

        val blockchairProvider by lazy {
            BlockchairNetworkProvider(blockchain, blockchainSdkConfig.blockchairApiKey)
        }
        providers.add(blockchairProvider)

        val blockcypherTokens = blockchainSdkConfig.blockcypherTokens
        if (!blockcypherTokens.isNullOrEmpty()) {
            val blockcypherProvider by lazy {
                BlockcypherNetworkProvider(blockchain, blockcypherTokens)
            }
            providers.add(blockcypherProvider)
        }
        return BitcoinNetworkService(providers)
    }

    private fun getToken(card: Card): Token? {
        val symbol = card.cardData?.tokenSymbol ?: return null
        val contractAddress = card.cardData?.tokenContractAddress ?: return null
        val decimals = card.cardData?.tokenDecimal ?: return null
        return Token(symbol, contractAddress, decimals)
    }
}