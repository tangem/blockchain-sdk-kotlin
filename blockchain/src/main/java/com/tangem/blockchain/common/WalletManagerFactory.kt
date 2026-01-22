package com.tangem.blockchain.common

import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.assembly.impl.*
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class WalletManagerFactory(
    private val config: BlockchainSdkConfig = BlockchainSdkConfig(),
    private val blockchainProviderTypes: Map<Blockchain, List<ProviderType>>,
    private val accountCreator: AccountCreator,
    featureToggles: BlockchainFeatureToggles,
    blockchainDataStorage: BlockchainDataStorage,
    loggers: List<BlockchainSDKLogger> = emptyList(),
) {

    private val dataStorage by lazy { AdvancedDataStorage(blockchainDataStorage) }

    init {
        DepsContainer.onInit(config, featureToggles)
        Logger.addLoggers(loggers)
    }

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

        val (addresses, finalPublicKey, finalPath) = when (blockchain) {
            Blockchain.Quai,
            Blockchain.QuaiTestnet,
            -> prepareQuaiWalletData(publicKey, blockchain, curve) ?: return null
            else -> prepareDefaultWalletData(publicKey, pairPublicKey, blockchain, curve)
        }

        val wallet = Wallet(blockchain, addresses, finalPublicKey, setOf(), finalPath)
        return getAssembly(blockchain).make(
            input = WalletManagerAssemblyInput(
                wallet = wallet,
                config = config,
                curve = curve,
                providerTypes = blockchainProviderTypes[blockchain] ?: emptyList(),
            ),
        )
    }

    private fun prepareQuaiWalletData(
        publicKey: Wallet.PublicKey,
        blockchain: Blockchain,
        curve: EllipticCurve,
    ): Triple<Set<Address>, Wallet.PublicKey, DerivationPath?>? {
        val extendedPublicKey = publicKey.derivationType?.hdKey?.extendedPublicKey ?: return null
        val cachedIndex = runBlocking(Dispatchers.IO) {
            dataStorage.getOrNull<BlockchainSavedData.QuaiDerivationIndex>(publicKey)
        }
        val updatedAddress = blockchain.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            curve = curve,
            cachedIndex = cachedIndex?.index,
        )
        updatedAddress.index?.let { index ->
            runBlocking(Dispatchers.IO) {
                dataStorage.store<BlockchainSavedData.QuaiDerivationIndex>(
                    publicKey = publicKey,
                    BlockchainSavedData.QuaiDerivationIndex(index),
                )
            }
        }
        val newPublicKey = Wallet.PublicKey(
            seedKey = publicKey.seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(
                    extendedPublicKey = updatedAddress.publicKey,
                    path = publicKey.derivationPath ?: return null,
                ),
            ),
        )
        return Triple(setOf(Address(updatedAddress.address)), newPublicKey, updatedAddress.path)
    }

    private fun prepareDefaultWalletData(
        publicKey: Wallet.PublicKey,
        pairPublicKey: ByteArray?,
        blockchain: Blockchain,
        curve: EllipticCurve,
    ): Triple<Set<Address>, Wallet.PublicKey, DerivationPath?> {
        val calculatedAddresses = blockchain.makeAddresses(publicKey.blockchainKey, pairPublicKey, curve)
        return Triple(calculatedAddresses, publicKey, null)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun getAssembly(blockchain: Blockchain): WalletManagerAssembly<WalletManager> {
        return when (blockchain) {
            // region BTC-like blockchains
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> BitcoinWalletManagerAssembly
            Blockchain.Dash -> DashWalletManagerAssembly
            Blockchain.Dogecoin -> DogecoinWalletManagerAssembly
            Blockchain.Litecoin -> LitecoinWalletManagerAssembly
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> BitcoinCashWalletManagerAssembly
            Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> RavencoinWalletManagerAssembly
            Blockchain.Ducatus -> DucatusWalletManagerAssembly
            Blockchain.Clore -> CloreWalletManagerAssembly
            Blockchain.Pepecoin, Blockchain.PepecoinTestnet -> PepecoinWalletManagerAssembly
            // endregion

            // region ETH-like blockchains
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> EthereumWalletManagerAssembly(dataStorage)

            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet,
            Blockchain.Avalanche, Blockchain.AvalancheTestnet,
            Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet,
            Blockchain.Fantom, Blockchain.FantomTestnet,
            Blockchain.RSK,
            Blockchain.BSC, Blockchain.BSCTestnet,
            Blockchain.Polygon, Blockchain.PolygonTestnet,
            Blockchain.Gnosis,
            Blockchain.Dischain,
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet,
            Blockchain.Kava, Blockchain.KavaTestnet,
            Blockchain.Cronos,
            Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet,
            Blockchain.Playa3ull,
            Blockchain.Shibarium, Blockchain.ShibariumTestnet,
            Blockchain.Aurora, Blockchain.AuroraTestnet,
            Blockchain.Areon, Blockchain.AreonTestnet,
            Blockchain.PulseChain, Blockchain.PulseChainTestnet,
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet,
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet,
            Blockchain.Moonriver, Blockchain.MoonriverTestnet,
            Blockchain.Flare, Blockchain.FlareTestnet,
            Blockchain.Taraxa, Blockchain.TaraxaTestnet,
            Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet,
            Blockchain.Core, Blockchain.CoreTestnet,
            Blockchain.Chiliz, Blockchain.ChilizTestnet,
            Blockchain.VanarChain, Blockchain.VanarChainTestnet,
            Blockchain.Xodex,
            Blockchain.Canxium,
            Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet,
            Blockchain.Bitrock, Blockchain.BitrockTestnet,
            Blockchain.Sonic, Blockchain.SonicTestnet,
            Blockchain.ApeChain, Blockchain.ApeChainTestnet,
            Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet,
            Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet,
            Blockchain.Quai, Blockchain.QuaiTestnet,
            Blockchain.Linea, Blockchain.LineaTestnet,
            Blockchain.ArbitrumNova,
            Blockchain.Plasma, Blockchain.PlasmaTestnet,
            -> EthereumLikeWalletManagerAssembly(dataStorage)

            Blockchain.Mantle, Blockchain.MantleTestnet,
            -> MantleWalletManagerAssembly

            Blockchain.Decimal, Blockchain.DecimalTestnet -> DecimalWalletManagerAssembly
            Blockchain.XDC, Blockchain.XDCTestnet -> XDCWalletManagerAssembly
            Blockchain.Optimism, Blockchain.OptimismTestnet,
            Blockchain.Base, Blockchain.BaseTestnet,
            Blockchain.Manta, Blockchain.MantaTestnet,
            Blockchain.Blast, Blockchain.BlastTestnet,
            Blockchain.Cyber, Blockchain.CyberTestnet,
            -> EthereumOptimisticRollupWalletManagerAssembly(dataStorage)
            Blockchain.Scroll, Blockchain.ScrollTestnet,
            -> ScrollWalletManagerAssembly(dataStorage)
            Blockchain.Telos, Blockchain.TelosTestnet -> TelosWalletManagerAssembly
            // endregion

            Blockchain.Solana, Blockchain.SolanaTestnet -> SolanaWalletManagerAssembly

            Blockchain.Polkadot, Blockchain.PolkadotTestnet,
            Blockchain.Kusama,
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet,
            Blockchain.Joystream,
            Blockchain.Bittensor,
            Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet,
            -> PolkadotWalletManagerAssembly

            Blockchain.Stellar, Blockchain.StellarTestnet -> StellarWalletManagerAssembly(dataStorage)
            Blockchain.Cardano -> CardanoWalletManagerAssembly
            Blockchain.XRP -> XRPWalletManagerAssembly(dataStorage)
            Blockchain.Binance, Blockchain.BinanceTestnet -> BinanceWalletManagerAssembly
            Blockchain.Tezos -> TezosWalletManagerAssembly
            Blockchain.Tron, Blockchain.TronTestnet -> TronWalletManagerAssembly
            Blockchain.Kaspa, Blockchain.KaspaTestnet -> KaspaWalletManagerAssembly(dataStorage)
            Blockchain.TON, Blockchain.TONTestnet -> TonWalletManagerAssembly
            Blockchain.Cosmos, Blockchain.CosmosTestnet -> CosmosWalletManagerAssembly
            Blockchain.TerraV1 -> TerraV1WalletManagerAssembly
            Blockchain.TerraV2 -> TerraV2WalletManagerAssembly
            Blockchain.Chia, Blockchain.ChiaTestnet -> ChiaWalletManagerAssembly
            Blockchain.Near, Blockchain.NearTestnet -> NearWalletManagerAssembly
            Blockchain.VeChain, Blockchain.VeChainTestnet -> VeChainWalletManagerAssembly
            Blockchain.Aptos, Blockchain.AptosTestnet -> AptosWalletManagerAssembly
            Blockchain.Algorand, Blockchain.AlgorandTestnet -> AlgorandWalletManagerAssembly
            Blockchain.Hedera, Blockchain.HederaTestnet -> HederaWalletManagerAssembly(dataStorage, accountCreator)
            Blockchain.Nexa, Blockchain.NexaTestnet -> NexaWalletManagerAssembly
            Blockchain.Radiant -> RadiantWalletManagerAssembly
            Blockchain.Fact0rn -> Fact0rnWalletManagerAssembly
            Blockchain.Koinos, Blockchain.KoinosTestnet -> KoinosWalletManagerAssembly
            Blockchain.Filecoin -> FilecoinWalletManagerAssembly
            Blockchain.Sei, Blockchain.SeiTestnet -> SeiWalletManagerAssembly
            Blockchain.InternetComputer -> ICPWalletManagerAssembly
            Blockchain.Sui, Blockchain.SuiTestnet -> SuiteWalletManagerAssembly
            Blockchain.Casper, Blockchain.CasperTestnet -> CasperWalletManagerAssembly
            Blockchain.Alephium, Blockchain.AlephiumTestnet -> AlephiumWalletManagerAssembly
            Blockchain.Unknown,
            -> error("Unsupported blockchain")
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