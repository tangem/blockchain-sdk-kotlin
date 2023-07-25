package com.tangem.blockchain.common

import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.assembly.impl.*
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import java.lang.IllegalStateException

class WalletManagerFactory(private val config: BlockchainSdkConfig) {

    fun createWalletManager(
        blockchain: Blockchain,
        publicKeys: Map<AddressType, Wallet.PublicKey>,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        val addressService = AddressServiceFactory(blockchain).makeAddressService()

        val walletFactory = WalletFactory(blockchain, addressService)
        val wallet = walletFactory.makeWallet(publicKeys = publicKeys, curve = curve)

        return createWalletManager(blockchain, wallet)
    }

    /**
     * Base wallet manager initializer
     * @param blockchain: blockchain to create
     * @param seedKey: Public key of the wallet
     * @param derivedKey: Derived ExtendedPublicKey by the card
     * @param derivationParams: derivation style or derivation path
     */
    fun createWalletManager(
        blockchain: Blockchain,
        seedKey: ByteArray,
        derivedKey: ExtendedPublicKey,
        derivationParams: DerivationParams,
    ): WalletManager? {

        val derivation: Wallet.Derivation? = when (derivationParams) {
            is DerivationParams.Custom -> {
                Wallet.Derivation(
                    derivedKey = derivedKey.publicKey,
                    derivationPath = derivationParams.path
                )
            }
            is DerivationParams.Default -> {
                val path = blockchain.derivationPath(derivationParams.style)
                path?.let {
                    Wallet.Derivation (
                        derivedKey = derivedKey.publicKey,
                        derivationPath = it
                    )
                }
            }
        }

        val publicKey = Wallet.PublicKey(seedKey = seedKey, derivation = derivation)
        val addressService = AddressServiceFactory(blockchain)
            .makeAddressService()

        val walletFactory = WalletFactory(blockchain, addressService)
        val wallet = walletFactory.makeWallet(publicKey, EllipticCurve.Secp256k1)

        return createWalletManager(
            blockchain = blockchain,
            wallet = wallet
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
        val publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivation = null)
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
        val publicKey = Wallet.PublicKey(seedKey = walletPublicKey, derivation = null)
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
            Blockchain.Telos, Blockchain.TelosTestnet
            -> {
                EthereumLikeWalletManagerAssembly
            }

            Blockchain.Optimism, Blockchain.OptimismTestnet -> {
                OptimismWalletManagerAssembly
            }

            // endregion

            Blockchain.Solana, Blockchain.SolanaTestnet -> {
                SolanaWalletManagerAssembly
            }

            Blockchain.Polkadot, Blockchain.PolkadotTestnet, Blockchain.Kusama,
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> {
                PolkadotWalletManagerAssembly
            }

            Blockchain.Stellar, Blockchain.StellarTestnet -> {
                StellarWalletManagerAssembly
            }

            Blockchain.Cardano, Blockchain.CardanoShelley -> {
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
