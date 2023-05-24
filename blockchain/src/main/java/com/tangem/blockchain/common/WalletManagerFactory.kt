package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.cardano.network.RosettaNetwork
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetworkProvider
import com.tangem.blockchain.blockchains.cosmos.CosmosWalletManager
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.blockchains.dogecoin.DogecoinWalletManager
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.kaspa.KaspaTransactionBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaWalletManager
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkService
import com.tangem.blockchain.blockchains.kaspa.network.KaspaRestApiNetworkProvider
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismWalletManager
import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.ravencoin.RavencoinWalletManager
import com.tangem.blockchain.blockchains.solana.SolanaRpcClientBuilder
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarNetwork
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkService
import com.tangem.blockchain.blockchains.ton.TonJsonRpcClientBuilder
import com.tangem.blockchain.blockchains.ton.TonWalletManager
import com.tangem.blockchain.blockchains.tron.TronTransactionBuilder
import com.tangem.blockchain.blockchains.tron.TronWalletManager
import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetwork
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_ADALITE
import com.tangem.blockchain.network.API_KASPA
import com.tangem.blockchain.network.API_RIPPLE
import com.tangem.blockchain.network.API_RIPPLE_RESERVE
import com.tangem.blockchain.network.API_TEZOS_BLOCKSCALE
import com.tangem.blockchain.network.API_TEZOS_ECAD
import com.tangem.blockchain.network.API_TEZOS_LETZBAKE
import com.tangem.blockchain.network.API_TEZOS_SMARTPY
import com.tangem.blockchain.network.API_XRP_LEDGER_FOUNDATION
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.network.blockscout.BlockscoutNetworkProvider
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

class WalletManagerFactory(
    private val config: BlockchainSdkConfig = BlockchainSdkConfig(),
) {

    /**
     * Base wallet manager initializer
     * @param blockchain: blockchain to create. If null, card native blockchain will be used
     * @param seedKey: Public Key of the wallet
     * @param derivedKey: Derived ExtendedPublicKey by the card
     * @param derivation: derivation style or derivation path
     */
    fun makeWalletManager(
        blockchain: Blockchain,
        seedKey: ByteArray,
        derivedKey: ExtendedPublicKey,
        derivation: DerivationParams,
    ): WalletManager? {
        val derivationPath: DerivationPath? = when (derivation) {
            is DerivationParams.Custom -> derivation.path
            is DerivationParams.Default -> blockchain.derivationPath(derivation.style)
        }

        return makeWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(
                seedKey = seedKey,
                derivedKey = derivedKey.publicKey,
                derivationPath = derivationPath
            )
        )
    }

    // Wallet manager initializer for twin cards
    fun makeTwinWalletManager(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        blockchain: Blockchain = Blockchain.Bitcoin,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        return makeWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(walletPublicKey, null, null),
            pairPublicKey = pairPublicKey,
            curve = curve
        )
    }

    // Legacy wallet manager initializer
    fun makeWalletManager(
        blockchain: Blockchain,
        walletPublicKey: ByteArray,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        return makeWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(walletPublicKey, null, null),
            curve = curve
        )
    }

    fun makeWalletManager(
        blockchain: Blockchain,
        publicKey: Wallet.PublicKey,
        tokens: Collection<Token> = emptyList(),
        pairPublicKey: ByteArray? = null,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        if (checkIfWrongKey(curve, publicKey)) return null

        val addresses = blockchain.makeAddresses(publicKey.blockchainKey, pairPublicKey, curve)
        val mutableTokens = tokens.toMutableSet()
        val wallet = Wallet(blockchain, addresses, publicKey, mutableTokens)

        return when (blockchain) {
            // region BTC-like blockchains
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Dash,
            -> {
                BitcoinWalletManager(
                    wallet = wallet,
                    transactionBuilder = BitcoinTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain,
                        walletAddresses = addresses
                    ),
                    networkProvider = BitcoinNetworkService(
                        providers = blockchain.getBitcoinNetworkProviders(blockchain, config)
                    )
                )
            }

            Blockchain.Dogecoin -> {
                DogecoinWalletManager(
                    wallet = wallet,
                    transactionBuilder = BitcoinTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain,
                        walletAddresses = addresses
                    ),
                    networkProvider = BitcoinNetworkService(
                        providers = blockchain.getBitcoinNetworkProviders(blockchain, config)
                    )
                )
            }

            Blockchain.Litecoin -> {
                LitecoinWalletManager(
                    wallet = wallet,
                    transactionBuilder = BitcoinTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain,
                        walletAddresses = addresses
                    ),
                    networkProvider = LitecoinNetworkService(
                        providers = blockchain.getBitcoinNetworkProviders(blockchain, config)
                    )
                )
            }

            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                BitcoinCashWalletManager(
                    wallet = wallet,
                    transactionBuilder = BitcoinCashTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = BitcoinNetworkService(
                        providers = blockchain.getBitcoinNetworkProviders(blockchain, config)
                    )
                )
            }
            Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> {
                RavencoinWalletManager(
                    wallet = wallet,
                    transactionBuilder = BitcoinTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain,
                        walletAddresses = addresses,
                    ),
                    networkProvider = BitcoinNetworkService(
                        providers = blockchain.getBitcoinNetworkProviders(blockchain, config)
                    )
                )
            }
            // endregion

            // region ETH-like blockchains
            Blockchain.Ethereum, Blockchain.EthereumClassic -> {
                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(
                        jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(config),
                        blockcypherNetworkProvider = BlockcypherNetworkProvider(
                            blockchain = blockchain,
                            tokens = config.blockcypherTokens
                        )
                    ),
                    presetTokens = mutableTokens
                )
            }

            Blockchain.Arbitrum,
            Blockchain.ArbitrumTestnet,
            Blockchain.Avalanche,
            Blockchain.AvalancheTestnet,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumClassicTestnet,
            Blockchain.Fantom,
            Blockchain.FantomTestnet,
            Blockchain.RSK,
            Blockchain.BSC,
            Blockchain.BSCTestnet,
            Blockchain.Polygon,
            Blockchain.PolygonTestnet,
            Blockchain.Gnosis,
            Blockchain.EthereumFair,
            Blockchain.EthereumPow,
            Blockchain.EthereumPowTestnet,
            Blockchain.Kava, Blockchain.KavaTestnet,
            -> {
                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(
                        jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(config)
                    ),
                    presetTokens = mutableTokens
                )
            }

            Blockchain.SaltPay -> {
                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(
                        jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(config),
                        blockscoutNetworkProvider = BlockscoutNetworkProvider(config.blockscoutCredentials),
                    ),
                    presetTokens = mutableTokens
                )
            }

            Blockchain.Optimism, Blockchain.OptimismTestnet -> {
                OptimismWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(
                        jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(config),
                    ),
                    presetTokens = mutableTokens
                )
            }
            // endregion

            Blockchain.Solana, Blockchain.SolanaTestnet -> {
                val clients = SolanaRpcClientBuilder().build(blockchain.isTestnet(), config)
                SolanaWalletManager(wallet, clients)
            }

            Blockchain.Ducatus -> {
                DucatusWalletManager(
                    wallet,
                    BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain),
                    DucatusNetworkService()
                )
            }

            Blockchain.Polkadot, Blockchain.PolkadotTestnet, Blockchain.Kusama -> {
                val network = PolkadotNetworkService.network(blockchain)

                PolkadotWalletManager(
                    wallet,
                    network,
                )
            }

            Blockchain.Stellar, Blockchain.StellarTestnet -> {
                val isTestnet = blockchain == Blockchain.StellarTestnet
                val hosts = if (!isTestnet) {
                    buildList {
                        config.nowNodeCredentials?.apiKey.letNotBlank {
                            add(StellarNetwork.Nownodes(it))
                        }
                        config.getBlockCredentials?.apiKey.letNotBlank {
                            add(StellarNetwork.Getblock(it))
                        }
                        add(StellarNetwork.Horizon)
                    }
                } else {
                    listOf<StellarNetwork>(StellarNetwork.HorizonTestnet)
                }
                val networkService = StellarNetworkService(hosts, isTestnet)

                StellarWalletManager(
                    wallet,
                    StellarTransactionBuilder(networkService, publicKey.blockchainKey),
                    networkService,
                    mutableTokens
                )
            }

            Blockchain.Cardano, Blockchain.CardanoShelley -> {
                val providers = buildList {
                    config.getBlockCredentials?.apiKey.letNotBlank {
                        add(RosettaNetworkProvider(RosettaNetwork.RosettaGetblock(it)))
                    }
                    add(AdaliteNetworkProvider(API_ADALITE))
                    add(RosettaNetworkProvider(RosettaNetwork.RosettaTangem))
                }

                CardanoWalletManager(
                    wallet,
                    CardanoTransactionBuilder(publicKey.blockchainKey),
                    CardanoNetworkService(providers)
                )
            }

            Blockchain.XRP -> {
                val rippledProvider1 = RippledNetworkProvider(API_XRP_LEDGER_FOUNDATION)
                val rippledProvider2 = RippledNetworkProvider(API_RIPPLE)
                val rippledProvider3 = RippledNetworkProvider(API_RIPPLE_RESERVE)
                val providers = listOf(rippledProvider1, rippledProvider2, rippledProvider3)
                val networkService = XrpNetworkService(providers)

                XrpWalletManager(
                    wallet,
                    XrpTransactionBuilder(networkService, publicKey.blockchainKey),
                    networkService
                )
            }

            Blockchain.Binance, Blockchain.BinanceTestnet -> {
                val isTestNet = blockchain == Blockchain.BinanceTestnet
                BinanceWalletManager(
                    wallet,
                    BinanceTransactionBuilder(publicKey.blockchainKey, isTestNet),
                    BinanceNetworkService(isTestNet),
                    mutableTokens
                )
            }

            Blockchain.Tezos -> {
                val providers = listOf(
                    TezosJsonRpcNetworkProvider(API_TEZOS_BLOCKSCALE),
                    TezosJsonRpcNetworkProvider(API_TEZOS_SMARTPY),
                    TezosJsonRpcNetworkProvider(API_TEZOS_ECAD),
                )

                TezosWalletManager(
                    wallet,
                    TezosTransactionBuilder(publicKey.blockchainKey, curve),
                    TezosNetworkService(providers),
                    curve
                )
            }

            Blockchain.Tron, Blockchain.TronTestnet -> {
                val networks = if (!blockchain.isTestnet()) {
                    buildList {
                        add(TronNetwork.TronGrid(null))
                        config.tronGridApiKey.letNotBlank {
                            add(TronNetwork.TronGrid(it))
                        }
                        config.nowNodeCredentials?.apiKey.letNotBlank {
                            add(TronNetwork.NowNodes(it))
                        }
                        config.getBlockCredentials?.apiKey.letNotBlank {
                            add(TronNetwork.GetBlock(it))
                        }
                    }
                } else {
                    listOf<TronNetwork>(TronNetwork.Nile)
                }
                val rpcProviders = networks.map {
                    TronJsonRpcNetworkProvider(network = it)
                }
                TronWalletManager(
                    wallet = wallet,
                    transactionBuilder = TronTransactionBuilder(blockchain),
                    networkService = TronNetworkService(rpcProviders, wallet.blockchain)
                )
            }

            Blockchain.Kaspa -> {
                val providers: List<KaspaNetworkProvider> = buildList {
                    add(KaspaRestApiNetworkProvider(API_KASPA))

                    config.kaspaSecondaryApiUrl
                        ?.takeIf { it.isNotBlank() }
                        ?.let { url ->
                            add(KaspaRestApiNetworkProvider("$url/"))
                        }
                }

                KaspaWalletManager(
                    wallet = wallet,
                    transactionBuilder = KaspaTransactionBuilder(),
                    networkProvider = KaspaNetworkService(providers)
                )
            }
            Blockchain.TON, Blockchain.TONTestnet -> {
                TonWalletManager(
                    wallet = wallet,
                    networkProviders = TonJsonRpcClientBuilder().build(blockchain.isTestnet(), config),
                )
            }
            Blockchain.Cosmos, Blockchain.CosmosTestnet -> {
                val testnet = blockchain.isTestnet()
                val providers = buildList {
                    if (testnet) {
                        add("https://rest.seed-01.theta-testnet.polypore.xyz")
                    } else {
                        config.nowNodeCredentials?.apiKey.letNotBlank { add("https://atom.nownodes.io/$it/") }
                        config.getBlockCredentials?.apiKey.letNotBlank { add("https://atom.getblock.io/$it/") }

                        add("https://cosmos-mainnet-rpc.allthatnode.com:1317/")
                        // This is a REST proxy combining the servers below (and others)
                        add("https://rest.cosmos.directory/cosmoshub/")
                        add("https://cosmoshub-api.lavenderfive.com/")
                        add("https://rest-cosmoshub.ecostake.com/")
                        add("https://lcd.cosmos.dragonstake.io/")
                    }
                }.map(::CosmosRestProvider)
                CosmosWalletManager(
                    wallet = wallet,
                    networkProviders = providers,
                    cosmosChain = CosmosChain.Cosmos(testnet)
                )
            }
            Blockchain.Unknown -> throw Exception("unsupported blockchain")
        }
    }

    private fun checkIfWrongKey(curve: EllipticCurve, publicKey: Wallet.PublicKey): Boolean {
        return when (curve) {
            EllipticCurve.Ed25519 -> publicKey.seedKey.size > 32 || publicKey.blockchainKey.size > 32
            else -> false
        }
    }
}