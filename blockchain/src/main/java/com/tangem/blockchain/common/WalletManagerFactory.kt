package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoNetworkProvider
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.blockchains.cardano.network.adalite.AdaliteNetworkProvider
import com.tangem.blockchain.blockchains.cardano.network.rosetta.RosettaNetworkProvider
import com.tangem.blockchain.blockchains.dogecoin.DogecoinWalletManager
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkService
import com.tangem.blockchain.blockchains.tron.TronTransactionBuilder
import com.tangem.blockchain.blockchains.tron.TronWalletManager
import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetwork
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.common.card.EllipticCurve
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey
import org.p2p.solanaj.rpc.Cluster

class WalletManagerFactory(
    private val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig()
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
        derivation: DerivationParams
    ): WalletManager? {


        var derivationPath: DerivationPath? =
            when (derivation) {
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
        curve: EllipticCurve = EllipticCurve.Secp256k1
    ): WalletManager? {
        return makeWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(walletPublicKey, null, null),
            pairPublicKey = pairPublicKey,
            curve = curve
        )
    }

    fun makeEthereumWalletManager(
        publicKey: Wallet.PublicKey,
        tokens: List<Token>,
        isTestNet: Boolean = false
    ): WalletManager? {
        val blockchain = if (isTestNet) Blockchain.EthereumTestnet else Blockchain.Ethereum
        val walletManager = makeWalletManager(blockchain, publicKey, tokens) ?: return null

        val additionalTokens = tokens.filterNot { walletManager.cardTokens.contains(it) }
        walletManager.cardTokens.addAll(additionalTokens)
        return walletManager
    }

    // Legacy wallet manager initializer
    fun makeWalletManager(
        blockchain: Blockchain,
        walletPublicKey: ByteArray,
        curve: EllipticCurve = EllipticCurve.Secp256k1
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
        curve: EllipticCurve = EllipticCurve.Secp256k1
    ): WalletManager? {

        if (checkIfWrongKey(curve, publicKey)) return null

        val addresses = blockchain.makeAddresses(publicKey.blockchainKey, pairPublicKey, curve)
        val tokens = tokens.toMutableSet()
        val wallet = Wallet(blockchain, addresses, publicKey, tokens)

        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet ->
                BitcoinWalletManager(
                    wallet,
                    BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain, addresses),
                    makeBitcoinNetworkService(blockchain)
                )

            Blockchain.Litecoin ->
                LitecoinWalletManager(
                    wallet,
                    BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain, addresses),
                    makeBitcoinNetworkService(blockchain)
                )

            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet ->
                BitcoinCashWalletManager(
                    wallet,
                    BitcoinCashTransactionBuilder(publicKey.blockchainKey, blockchain),
                    BitcoinCashNetworkService(
                        blockchairApiKey = blockchainSdkConfig.blockchairApiKey,
                        blockchairAuthorizationToken = blockchainSdkConfig.blockchairAuthorizationToken
                    )
                )

            Blockchain.Dogecoin ->
                DogecoinWalletManager(
                    wallet,
                    BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain),
                    makeBitcoinNetworkService(blockchain)
                )

            Blockchain.Ducatus ->
                DucatusWalletManager(
                    wallet,
                    BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain),
                    DucatusNetworkService()
                )

            Blockchain.ArbitrumTestnet -> {
                val jsonRpcProviders = mutableListOf<EthereumJsonRpcProvider>()
                jsonRpcProviders.add(EthereumJsonRpcProvider.classic(API_ARBITRUM_TESTNET, "rpc/"))

                val networkService = EthereumNetworkService(jsonRpcProviders)

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    networkService,
                    tokens
                )
            }

            Blockchain.Arbitrum -> {
                val jsonRpcProviders = mutableListOf<EthereumJsonRpcProvider>()
                jsonRpcProviders.add(
                    EthereumJsonRpcProvider
                        .classic(API_ARBITRUM, "rpc/")
                )
                if (blockchainSdkConfig.infuraProjectId != null) {
                    jsonRpcProviders.add(
                        EthereumJsonRpcProvider.infura(
                            API_ARBITRUM_INFURA,
                            blockchainSdkConfig.infuraProjectId
                        )
                    )
                }
                jsonRpcProviders.add(EthereumJsonRpcProvider(API_ARBITRUM_OFFCHAIN))

                val networkService = EthereumNetworkService(jsonRpcProviders)

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    networkService,
                    tokens
                )
            }
            Blockchain.EthereumClassic -> {
                val jsonRpcProviders = listOf(
                    EthereumJsonRpcProvider.classic(API_ETH_CLASSIC_CLUSTER, "etc"),
                    EthereumJsonRpcProvider(API_ETH_CLASSIC_BLOCKSCOUT),
                    EthereumJsonRpcProvider(API_ETH_CLASSIC_ETCDESKTOP),
                    EthereumJsonRpcProvider(API_ETH_CLASSIC_MYTOKEN),
                    EthereumJsonRpcProvider(API_ETH_CLASSIC_BESU),
                    EthereumJsonRpcProvider(API_ETH_CLASSIC_GETH),
                )
                val blockchairEthNetworkProvider = BlockchairEthNetworkProvider(
                    apiKey = blockchainSdkConfig.blockchairApiKey,
                    authorizationToken = blockchainSdkConfig.blockchairAuthorizationToken
                )
                val blockcypherNetworkProvider =
                    BlockcypherNetworkProvider(blockchain, blockchainSdkConfig.blockcypherTokens)

                val networkService = EthereumNetworkService(
                    jsonRpcProviders,
                    blockcypherNetworkProvider,
                    blockchairEthNetworkProvider
                )

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    networkService,
                    tokens
                )
            }
            Blockchain.EthereumClassicTestnet -> {
                val networkService = EthereumNetworkService(listOf(
                    EthereumJsonRpcProvider(API_ETH_CLASSIC_CLUSTER, "kotti")
                ))

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    networkService,
                    tokens
                )
            }
            Blockchain.Ethereum -> {
                val jsonRpcProviders = mutableListOf<EthereumJsonRpcProvider>()
                if (blockchainSdkConfig.infuraProjectId != null) {
                    jsonRpcProviders.add(
                        EthereumJsonRpcProvider
                            .infura(API_INFURA, blockchainSdkConfig.infuraProjectId)
                    )
                }

                val blockchairEthNetworkProvider = BlockchairEthNetworkProvider(
                    apiKey = blockchainSdkConfig.blockchairApiKey,
                    authorizationToken = blockchainSdkConfig.blockchairAuthorizationToken
                )
                val blockcypherNetworkProvider =
                    BlockcypherNetworkProvider(blockchain, blockchainSdkConfig.blockcypherTokens)

                val networkService = EthereumNetworkService(
                    jsonRpcProviders,
                    blockcypherNetworkProvider,
                    blockchairEthNetworkProvider
                )

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    networkService,
                    tokens
                )
            }
            Blockchain.EthereumTestnet -> {
                val jsonRpcProvider = EthereumJsonRpcProvider.infura(
                    API_INFURA_TESTNET, blockchainSdkConfig.infuraProjectId
                    ?: throw Exception("Infura project Id is required")
                )
                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    EthereumNetworkService(listOf(jsonRpcProvider)),
                    tokens
                )
            }
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> {
                val api =
                    if (blockchain == Blockchain.Avalanche) API_AVALANCHE else API_AVALANCHE_TESTNET

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    EthereumNetworkService(listOf(EthereumJsonRpcProvider(api, "ext/bc/C/rpc"))),
                    tokens
                )
            }
            Blockchain.Fantom, Blockchain.FantomTestnet -> {
                val providers = mutableListOf<EthereumJsonRpcProvider>()
                if (blockchain.isTestnet()) {
                    providers.add(EthereumJsonRpcProvider(API_FANTOM_TESTNET))
                } else {
                    providers.add(EthereumJsonRpcProvider(API_FANTOM_NETWORK))
                    providers.add(EthereumJsonRpcProvider(API_FANTOM_ULTIMATENODES))
                    providers.add(EthereumJsonRpcProvider(API_FANTOM_TOOLS))
                    providers.add(EthereumJsonRpcProvider(API_FANTOM_ANKR_TOOLS, "ftm"))
                }

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    EthereumNetworkService(providers),
                    tokens
                )
            }
            Blockchain.RSK -> {
                val jsonRpcProvider = EthereumJsonRpcProvider(API_RSK)

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    EthereumNetworkService(listOf(jsonRpcProvider)),
                    tokens
                )
            }
            Blockchain.BSC, Blockchain.BSCTestnet -> {
                val api = if (blockchain == Blockchain.BSC) API_BSC else API_BSC_TESTNET
                val jsonRpcProvider = EthereumJsonRpcProvider(api)

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    EthereumNetworkService(listOf(jsonRpcProvider)),
                    tokens
                )
            }
            Blockchain.Polygon, Blockchain.PolygonTestnet -> {
                val jsonRpcProviders = if (blockchain == Blockchain.Polygon) {
                    listOf(
                        EthereumJsonRpcProvider(API_POLYGON),
                        EthereumJsonRpcProvider(API_POLYGON_MATICVIGIL),
                    )

                } else {
                    listOf(EthereumJsonRpcProvider(API_POLYGON_TESTNET))
                }

                EthereumWalletManager(
                    wallet,
                    EthereumTransactionBuilder(publicKey.blockchainKey, blockchain),
                    EthereumNetworkService(jsonRpcProviders),
                    tokens
                )
            }
            Blockchain.Stellar, Blockchain.StellarTestnet -> {
                val isTestnet = blockchain == Blockchain.StellarTestnet
                val networkService = StellarNetworkService(isTestnet)

                StellarWalletManager(
                    wallet,
                    StellarTransactionBuilder(networkService, publicKey.blockchainKey),
                    networkService,
                    tokens
                )
            }
            Blockchain.Solana, Blockchain.SolanaTestnet -> {
                val clients = when (blockchain) {
                    Blockchain.Solana -> listOf(
                        RpcClient("https://solana-api.projectserum.com"),
                        RpcClient("https://rpc.ankr.com/solana"),
                        RpcClient(Cluster.MAINNET.endpoint),
                        )
                    else -> listOf(RpcClient(Cluster.DEVNET.endpoint))
                }
                SolanaWalletManager(wallet, clients)
            }
            Blockchain.Cardano, Blockchain.CardanoShelley -> {
                val adaliteNetworkProvider = AdaliteNetworkProvider(API_ADALITE)
                val rosettaNetworkProvider = RosettaNetworkProvider(API_TANGEM_ROSETTA)
                val providers = listOf(adaliteNetworkProvider, rosettaNetworkProvider)

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
                    tokens
                )
            }
            Blockchain.Tezos -> {
                val tezosProvider1 = TezosJsonRpcNetworkProvider(API_TEZOS_LETZBAKE)
                val tezosProvider2 = TezosJsonRpcNetworkProvider(API_TEZOS_BLOCKSCALE)
                val tezosProvider3 = TezosJsonRpcNetworkProvider(API_TEZOS_SMARTPY)
                val tezosProvider4 = TezosJsonRpcNetworkProvider(API_TEZOS_ECAD)
                val providers =
                    listOf(tezosProvider1, tezosProvider2, tezosProvider3, tezosProvider4)

                TezosWalletManager(
                    wallet,
                    TezosTransactionBuilder(publicKey.blockchainKey, curve),
                    TezosNetworkService(providers),
                    curve
                )
            }
            Blockchain.Tron, Blockchain.TronTestnet -> {
                val network = if (blockchain.isTestnet()) TronNetwork.NILE else TronNetwork.MAINNET
                val rpcProvider = TronJsonRpcNetworkProvider(
                    network = network, tronGridApiKey = blockchainSdkConfig.tronGridApiKey
                )
                TronWalletManager(
                    wallet = wallet,
                    transactionBuilder = TronTransactionBuilder(blockchain),
                    networkProvider = rpcProvider
                )
            }
            Blockchain.Gnosis -> {
                val jsonRpcProviders = listOf(
                    EthereumJsonRpcProvider(API_GNOSIS_CHAIN),
                    EthereumJsonRpcProvider(API_GNOSIS_POKT),
                    EthereumJsonRpcProvider(API_GNOSIS_ANKR),
                    EthereumJsonRpcProvider(API_GNOSIS_BLAST),
                    EthereumJsonRpcProvider(API_XDAI_POKT),
                    EthereumJsonRpcProvider(API_XDAI_BLOCKSCOUT),
                )

                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(jsonRpcProviders),
                    presetToken = tokens
                )
            }
            Blockchain.EthereumFair -> {
                val jsonRpcProviders = listOf(EthereumJsonRpcProvider(API_ETH_FAIR_RPC))
                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(jsonRpcProviders),
                    presetToken = tokens
                )
            }
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> {
                val api = if (blockchain.isTestnet()) API_ETH_POW_TESTNET_RPC else API_ETH_POW_RPC
                val jsonRpcProviders = listOf(EthereumJsonRpcProvider(api))

                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(jsonRpcProviders),
                    presetToken = tokens
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

    private fun makeBitcoinNetworkService(blockchain: Blockchain): BitcoinNetworkService {
        val providers = mutableListOf<BitcoinNetworkProvider>()

        if (blockchain == Blockchain.Bitcoin) providers.add(BlockchainInfoNetworkProvider())

        providers.add(
            BlockchairNetworkProvider(
                blockchain = blockchain,
                apiKey = blockchainSdkConfig.blockchairApiKey,
                authorizationToken = blockchainSdkConfig.blockchairAuthorizationToken
            )
        )

        val blockcypherTokens = blockchainSdkConfig.blockcypherTokens
        if (!blockcypherTokens.isNullOrEmpty()) {
            providers.add(BlockcypherNetworkProvider(blockchain, blockcypherTokens))
        }
        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.Dogecoin ->
                BitcoinNetworkService(providers)
            Blockchain.Litecoin -> LitecoinNetworkService(providers)
            else -> {
                throw Exception(
                    blockchain.name + " blockchain is not supported by BitcoinNetworkService"
                )
            }
        }
    }
}
