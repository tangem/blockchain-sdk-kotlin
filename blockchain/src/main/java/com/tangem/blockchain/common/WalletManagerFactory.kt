package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoNetworkProvider
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
import com.tangem.blockchain.blockchains.ethereum.getEthereumJsonRpcProviders
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismWalletManager
import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.solana.SolanaRpcClientBuilder
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
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
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.common.card.EllipticCurve
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey

class WalletManagerFactory(
    private val config: BlockchainSdkConfig = BlockchainSdkConfig()
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
                    makeBitcoinNetworkService(Blockchain.BitcoinCash),
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

            Blockchain.Polkadot, Blockchain.PolkadotTestnet, Blockchain.Kusama -> {
                val network = PolkadotNetworkService.network(blockchain)

                PolkadotWalletManager(
                    wallet,
                    network,
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
                val clients = SolanaRpcClientBuilder().build(blockchain.isTestnet(), config)
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
                val providers = listOf(
                    TezosJsonRpcNetworkProvider(API_TEZOS_LETZBAKE),
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
                val network = if (blockchain.isTestnet()) TronNetwork.NILE else TronNetwork.MAINNET
                val rpcProvider = TronJsonRpcNetworkProvider(
                    network = network,
                    tronGridApiKey = config.tronGridApiKey
                )
                TronWalletManager(
                    wallet = wallet,
                    transactionBuilder = TronTransactionBuilder(blockchain),
                    networkProvider = rpcProvider
                )
            }
            Blockchain.Dash -> {
                BitcoinWalletManager(
                    wallet,
                    BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain, addresses),
                    makeBitcoinNetworkService(blockchain)
                )
            }

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
                    presetTokens = tokens
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
            Blockchain.SaltPay -> {
                EthereumWalletManager(
                    wallet = wallet,
                    transactionBuilder = EthereumTransactionBuilder(
                        walletPublicKey = publicKey.blockchainKey,
                        blockchain = blockchain
                    ),
                    networkProvider = EthereumNetworkService(
                        jsonRpcProviders = blockchain.getEthereumJsonRpcProviders(config)
                    ),
                    presetTokens = tokens
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
                    presetTokens = tokens
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

        config.blockchairCredentials?.let { blockchairCredentials ->
            blockchairCredentials.apiKey.forEach { apiKey ->
                providers.add(
                    BlockchairNetworkProvider(
                        blockchain = blockchain,
                        apiKey = apiKey,
                        authorizationToken = blockchairCredentials.authToken,
                    )
                )
            }
        }

        if (blockchain != Blockchain.BitcoinCash && blockchain != Blockchain.BitcoinCashTestnet
            && !config.blockcypherTokens.isNullOrEmpty()) {
            providers.add(BlockcypherNetworkProvider(blockchain, config.blockcypherTokens))
        }
        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.Dogecoin, Blockchain.Dash,
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> BitcoinNetworkService(providers)
            Blockchain.Litecoin -> LitecoinNetworkService(providers)
            else -> {
                throw Exception(
                    blockchain.name + " blockchain is not supported by BitcoinNetworkService"
                )
            }
        }
    }
}
