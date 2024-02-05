package com.tangem.blockchain.common

import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.assembly.impl.*
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.common.card.EllipticCurve

class WalletManagerFactory(
    private val config: BlockchainSdkConfig = BlockchainSdkConfig(),
    blockchainDataStorage: BlockchainDataStorage,
) {

    @Suppress("UnusedPrivateMember")
    private val dataStorage by lazy { AdvancedDataStorage(blockchainDataStorage) }

    /**
     * Base wallet manager initializer
     *
     * @param blockchain blockchain to create
     * @param publicKey  public key of the wallet
     * @param curve      optional curve to generate addresses for some blockchains
     */
    fun createWalletManager(
        blockchain: Blockchain,
        publicKey: Wallet.PublicKey,
        curve: EllipticCurve,
    ): WalletManager? {
        return createWalletManager(
            blockchain = blockchain,
            publicKey = publicKey,
            pairPublicKey = null,
            curve = curve,
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
        return createWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(walletPublicKey, null),
            pairPublicKey = pairPublicKey,
            curve = curve,
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
        return createWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(walletPublicKey, null),
            curve = curve,
        )
    }

    private fun createWalletManager(
        blockchain: Blockchain,
        publicKey: Wallet.PublicKey,
        pairPublicKey: ByteArray? = null,
        curve: EllipticCurve = EllipticCurve.Secp256k1,
    ): WalletManager? {
        if (checkIfWrongKey(blockchain, curve, publicKey)) return null

        val addresses = blockchain.makeAddresses(publicKey.blockchainKey, pairPublicKey, curve)
        val wallet = Wallet(blockchain, addresses, publicKey, setOf())

        return getAssembly(blockchain).make(
            input = WalletManagerAssemblyInput(
                wallet = wallet,
                config = config,
                curve = curve,
            ),
        )
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun getAssembly(blockchain: Blockchain): WalletManagerAssembly<WalletManager> {
        return when (blockchain) {
            // region BTC-like blockchains

            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            -> {
                BitcoinWalletManagerAssembly
            }

            Blockchain.Dash -> {
                DashWalletManagerAssembly
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

            Blockchain.Decimal, Blockchain.DecimalTestnet -> {
                DecimalWalletManagerAssembly
            }

            Blockchain.XDC, Blockchain.XDCTestnet -> {
                XDCWalletManagerAssembly
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

            Blockchain.Near, Blockchain.NearTestnet -> {
                NearWalletManagerAssembly
            }

            Blockchain.VeChain, Blockchain.VeChainTestnet -> {
                VeChainWalletManagerAssembly
            }

            Blockchain.Aptos, Blockchain.AptosTestnet -> {
                AptosWalletManagerAssembly
            }

            Blockchain.Unknown -> {
                error("Unsupported blockchain")
            }
        }
    }

    @Suppress("MagicNumber")
    private fun checkIfWrongKey(blockchain: Blockchain, curve: EllipticCurve, publicKey: Wallet.PublicKey): Boolean {
        // wallet2 has cardano with extended key, so we should take this into account
        return when (curve) {
            EllipticCurve.Ed25519 ->
                publicKey.seedKey.size > 32 || publicKey.blockchainKey.size > 32 && blockchain != Blockchain.Cardano

            else -> false
        }
    }
}
