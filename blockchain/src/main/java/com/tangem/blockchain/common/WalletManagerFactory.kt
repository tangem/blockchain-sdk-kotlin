package com.tangem.blockchain.common

import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.assembly.impl.*
import com.tangem.common.card.EllipticCurve

class WalletManagerFactory(private val config: BlockchainSdkConfig = BlockchainSdkConfig()) {

    /**
     * Base wallet manager initializer
     * @param blockchain: blockchain to create
     * @param publicKey: Public Key of the wallet
     */
    fun createWalletManager(
        blockchain: Blockchain,
        publicKey: Wallet.PublicKey
    ): WalletManager? {

        val addressService = AddressServiceFactory(blockchain)
            .makeAddressService()

        val walletFactory = WalletFactory(blockchain, addressService)
        val wallet = walletFactory.makeWallet(publicKey, EllipticCurve.Secp256k1)

        return createWalletManager(
            blockchain = blockchain,
            wallet = wallet,
            pairPublicKey = null,
            curve = EllipticCurve.Secp256k1
        )
    }

    /**
     * Creates manager initializer for twin cards
     * @param walletPublicKey: Public Key of the wallet
     * @param pairPublicKey: Derived ExtendedPublicKey by the card
     * @param blockchain: blockchain to create.
     * @param curve: Card curve
     */
    fun createTwinWalletManager(
        walletPublicKey: ByteArray,
        pairPublicKey: ByteArray,
        blockchain: Blockchain = Blockchain.Bitcoin,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        val publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null)
        val addressService = AddressServiceFactory(blockchain).makeAddressService()

        val walletFactory = WalletFactory(blockchain, addressService)
        val wallet = walletFactory.makeWallet(publicKey, pairPublicKey)

        return createWalletManager(
            blockchain = blockchain,
            wallet = wallet,
            pairPublicKey = pairPublicKey,
            curve = curve
        )
    }

    /**
     * Legacy wallet manager initializer
     * @param blockchain: Blockhain to create.
     * @param walletPublicKey Wallet's publicKey
     * @param curve: card curve
     */
    fun createLegacyWalletManager(
        blockchain: Blockchain,
        walletPublicKey: ByteArray,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        val publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivationType = null)
        val addressService = AddressServiceFactory(blockchain).makeAddressService()

        val walletFactory = WalletFactory(blockchain, addressService)
        val wallet = walletFactory.makeWallet(publicKey, curve)

        return createWalletManager(
            blockchain = blockchain,
            wallet = wallet,
            curve = curve
        )
    }

    private fun createWalletManager(
        blockchain: Blockchain,
        wallet: Wallet,
        pairPublicKey: ByteArray? = null,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        if (checkIfWrongKey(curve, wallet.publicKey)) return null

        return getAssembly(blockchain).make(
            input = WalletManagerAssemblyInput(
                wallet = wallet,
                config = config,
                curve = curve,
                pairPublicKey = pairPublicKey
            )
        )
    }

    private fun getAssembly(blockchain: Blockchain): WalletManagerAssembly<WalletManager> {
        return when (blockchain) {
            // region BTC-like blockchains

            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Dash,
            -> {
                BitcoinWalletManagerAssembly
            }

            Blockchain.Dogecoin -> {
                DogecoinWalletManagerAssembly
            }

            Blockchain.Litecoin -> {
                LitecoinWalletManagerAssembly
            }

            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                BitcoinCashWalletManagerAssembly
            }

            Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> {
                RavencoinWalletManagerAssembly
            }

            Blockchain.Ducatus -> {
                DucatusWalletManagerAssembly
            }

            // endregion

            // region ETH-like blockchains

            Blockchain.Ethereum, Blockchain.EthereumClassic -> {
                EthereumWalletManagerAssembly
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
            Blockchain.Cronos,
            Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet,
            -> {
                EthereumLikeWalletManagerAssembly
            }

            Blockchain.Optimism, Blockchain.OptimismTestnet -> {
                OptimismWalletManagerAssembly
            }

            Blockchain.Telos, Blockchain.TelosTestnet -> {
                TelosWalletManagerAssembly
            }

            // endregion

            Blockchain.Solana, Blockchain.SolanaTestnet -> {
                SolanaWalletManagerAssembly
            }

            Blockchain.Polkadot, Blockchain.PolkadotTestnet, Blockchain.Kusama,
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet,
            -> {
                PolkadotWalletManagerAssembly
            }

            Blockchain.Stellar, Blockchain.StellarTestnet -> {
                StellarWalletManagerAssembly
            }

            Blockchain.Cardano -> {
                CardanoWalletManagerAssembly
            }

            Blockchain.XRP -> {
                XRPWalletManagerAssembly
            }

            Blockchain.Binance, Blockchain.BinanceTestnet -> {
                BinanceWalletManagerAssembly
            }

            Blockchain.Tezos -> {
                TezosWalletManagerAssembly
            }

            Blockchain.Tron, Blockchain.TronTestnet -> {
                TronWalletManagerAssembly
            }

            Blockchain.Kaspa -> {
                KaspaWalletManagerAssembly
            }

            Blockchain.TON, Blockchain.TONTestnet -> {
                TonWalletManagerAssembly
            }

            Blockchain.Cosmos, Blockchain.CosmosTestnet -> {
                CosmosWalletManagerAssembly
            }

            Blockchain.TerraV1 -> {
                TerraV1WalletManagerAssembly
            }

            Blockchain.TerraV2 -> {
                TerraV2WalletManagerAssembly
            }

            Blockchain.Chia, Blockchain.ChiaTestnet -> {
                ChiaWalletManagerAssembly
            }

            Blockchain.Unknown -> {
                throw IllegalStateException("Unsupported blockchain")
            }
        }
    }

    private fun checkIfWrongKey(curve: EllipticCurve, publicKey: Wallet.PublicKey): Boolean {
        return when (curve) {
            EllipticCurve.Ed25519 -> {
                publicKey.seedKey.size > 32 || publicKey.blockchainKey.size > 32
            }
            else -> false
        }
    }
}
