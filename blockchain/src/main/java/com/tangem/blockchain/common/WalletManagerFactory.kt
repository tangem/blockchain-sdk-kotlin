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
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetworkProvider
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
import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkService
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.extensions.getBlockchain
import com.tangem.blockchain.extensions.getToken
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.EllipticCurve

class WalletManagerFactory(
        private val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig()
) {

    fun makeWalletManager(card: Card, blockchain: Blockchain? = null): WalletManager? {
        val selectedBlockchain = blockchain ?: card.getBlockchain() ?: return null
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val curve = card.curve ?: return null
        val cardId = card.cardId
        val presetTokens = card.getToken()?.let { mutableSetOf(it) } ?: mutableSetOf()

        return makeWalletManager(selectedBlockchain, walletPublicKey, cardId, curve, tokens = presetTokens)
    }

    fun makeWalletManagers(card: Card, blockchains: List<Blockchain>): List<WalletManager> {
        return blockchains.mapNotNull { makeWalletManager(card, it) }
    }

    fun makeEthereumWalletManager(card: Card, tokens: List<Token>): WalletManager? {
        val walletManager = makeWalletManager(card, Blockchain.Ethereum) ?: return null
        val additionalTokens = tokens.filterNot { walletManager.presetTokens.contains(it) }
        walletManager.presetTokens.addAll(additionalTokens)
        return walletManager
    }

    fun makeMultisigWalletManager(
            card: Card, pairPublicKey: ByteArray,
    ): WalletManager? {
        val walletPublicKey: ByteArray = card.walletPublicKey ?: return null
        val blockchain = card.getBlockchain() ?: return null
        val curve = card.curve ?: return null
        val cardId = card.cardId

        return makeWalletManager(
                blockchain, walletPublicKey, cardId, curve, pairPublicKey
        )
    }

    private fun makeWalletManager(
            blockchain: Blockchain,
            walletPublicKey: ByteArray,
            cardId: String,
            cardCurve: EllipticCurve,
            walletPairPublickKey: ByteArray? = null,
            tokens: MutableSet<Token> = mutableSetOf()
    ): WalletManager {

        val addresses = blockchain.makeAddresses(walletPublicKey, walletPairPublickKey, cardCurve)

        val wallet = Wallet(blockchain, addresses, tokens)

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
                        tokens
                )
            }
            Blockchain.EthereumTestnet -> {
                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkService(blockchain, blockchainSdkConfig.infuraProjectId),
                        tokens
                )
            }
            Blockchain.RSK -> {
                EthereumWalletManager(
                        cardId, wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkService(blockchain, null),
                        tokens
                )
            }
            Blockchain.Stellar -> {
                val networkService = StellarNetworkService()

                StellarWalletManager(
                        cardId, wallet,
                        StellarTransactionBuilder(networkService, walletPublicKey),
                        networkService,
                        tokens
                )
            }
            Blockchain.Cardano, Blockchain.CardanoShelley -> {
                val adaliteNetworkProvider1 by lazy { AdaliteNetworkProvider(API_ADALITE) }
                val rosettaNetworkProvider by lazy { RosettaNetworkProvider(API_TANGEM_ROSETTA) }
                val providers = listOf(
                        adaliteNetworkProvider1,
                        rosettaNetworkProvider
                )

                CardanoWalletManager(
                        cardId, wallet,
                        CardanoTransactionBuilder(walletPublicKey),
                        CardanoNetworkService(providers)
                )
            }
            Blockchain.XRP -> {
                val rippledProvider1 by lazy { RippledNetworkProvider(API_RIPPLED) }
                val rippledProvider2 by lazy { RippledNetworkProvider(API_RIPPLED_RESERVE) }
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
                        tokens
                )
            }
            Blockchain.BinanceTestnet -> {
                BinanceWalletManager(
                        cardId, wallet,
                        BinanceTransactionBuilder(walletPublicKey, true),
                        BinanceNetworkService(true),
                        tokens
                )
            }
            Blockchain.Tezos -> {
                val tezosProvider1 by lazy { TezosJsonRpcNetworkProvider(API_TEZOS_LETZBAKE) }
                val tezosProvider2 by lazy { TezosJsonRpcNetworkProvider(API_TEZOS_BLOCKSCALE) }
                val tezosProvider3 by lazy { TezosJsonRpcNetworkProvider(API_TEZOS_SMARTPY) }
                val tezosProvider4 by lazy { TezosJsonRpcNetworkProvider(API_TEZOS_ECAD) }
                val providers = listOf(tezosProvider1, tezosProvider2, tezosProvider3, tezosProvider4)

                TezosWalletManager(
                        cardId, wallet,
                        TezosTransactionBuilder(walletPublicKey),
                        TezosNetworkService(providers)
                )
            }
            Blockchain.Unknown -> throw Exception("unsupported blockchain")
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
}