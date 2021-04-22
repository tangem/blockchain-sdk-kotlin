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
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.commands.common.card.EllipticCurve

class WalletManagerFactory(
        private val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig()
) {

    fun makeWalletManager(
            cardId: String, walletPublicKey: ByteArray, blockchain: Blockchain,
            curve: EllipticCurve = EllipticCurve.Secp256k1
    ): WalletManager? {
        return makeWalletManager(
                cardId = cardId, walletPublicKey = walletPublicKey,
                blockchain = blockchain, curve = curve, walletPairPublickKey = null
        )
    }

    fun makeWalletManagers(
            cardId: String, walletPublicKey: ByteArray, blockchains: List<Blockchain>,
            curve: EllipticCurve = EllipticCurve.Secp256k1
    ): List<WalletManager> {
        return blockchains.mapNotNull { blockchain ->
            makeWalletManager(
                    cardId = cardId, walletPublicKey = walletPublicKey,
                    blockchain = blockchain, curve = curve
            )
        }
    }

    fun makeEthereumWalletManager(
            cardId: String, walletPublicKey: ByteArray, tokens: List<Token>,
            isTestNet: Boolean = false
    ): WalletManager? {
        val blockchain = if (isTestNet) Blockchain.EthereumTestnet else Blockchain.Ethereum
        val walletManager =
                makeWalletManager(cardId, walletPublicKey, blockchain, tokens) ?: return null
        val additionalTokens = tokens.filterNot { walletManager.presetTokens.contains(it) }
        walletManager.presetTokens.addAll(additionalTokens)
        return walletManager
    }

    fun makeMultisigWalletManager(
            cardId: String, walletPublicKey: ByteArray, pairPublicKey: ByteArray,
            blockchain: Blockchain = Blockchain.Bitcoin,
            curve: EllipticCurve = EllipticCurve.Secp256k1
    ): WalletManager? {
        return makeWalletManager(
                cardId = cardId, walletPublicKey = walletPublicKey, blockchain,
                walletPairPublickKey = pairPublicKey, curve = curve
        )
    }


    internal fun makeWalletManager(
            cardId: String,
            walletPublicKey: ByteArray,
            blockchain: Blockchain,
            tokens: List<Token> = emptyList(),
            walletPairPublickKey: ByteArray? = null,
            curve: EllipticCurve = EllipticCurve.Secp256k1
    ): WalletManager? {

        if (curve == EllipticCurve.Ed25519 && walletPublicKey.size > 32) return null //wrong key

        val addresses = blockchain.makeAddresses(walletPublicKey, walletPairPublickKey, curve)

        val tokens = tokens.toMutableSet()
        val wallet = Wallet(cardId, blockchain, addresses, walletPublicKey, tokens)

        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> {
                BitcoinWalletManager(
                        wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkService(blockchain)
                )
            }

            Blockchain.Litecoin -> {
                LitecoinWalletManager(
                        wallet,
                        BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses),
                        makeBitcoinNetworkService(blockchain)
                )
            }

            Blockchain.BitcoinCash -> {
                BitcoinCashWalletManager(
                        wallet,
                        BitcoinCashTransactionBuilder(walletPublicKey, blockchain),
                        BitcoinCashNetworkService(blockchainSdkConfig.blockchairApiKey)
                )
            }
            Blockchain.Ducatus -> {
                DucatusWalletManager(
                        wallet,
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
                        wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        networkService,
                        tokens
                )
            }
            Blockchain.EthereumTestnet -> {
                EthereumWalletManager(
                        wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkService(blockchain, blockchainSdkConfig.infuraProjectId),
                        tokens
                )
            }
            Blockchain.RSK -> {
                EthereumWalletManager(
                        wallet,
                        EthereumTransactionBuilder(walletPublicKey, blockchain),
                        EthereumNetworkService(blockchain, null),
                        tokens
                )
            }
            Blockchain.Stellar -> {
                val networkService = StellarNetworkService()

                StellarWalletManager(
                        wallet,
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
                        wallet,
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
                        wallet,
                        XrpTransactionBuilder(networkService, walletPublicKey),
                        networkService
                )
            }
            Blockchain.Binance -> {
                BinanceWalletManager(
                        wallet,
                        BinanceTransactionBuilder(walletPublicKey),
                        BinanceNetworkService(),
                        tokens
                )
            }
            Blockchain.BinanceTestnet -> {
                BinanceWalletManager(
                        wallet,
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
                        wallet,
                        TezosTransactionBuilder(walletPublicKey, curve),
                        TezosNetworkService(providers),
                        curve
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