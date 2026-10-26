package com.tangem.blockchain.blockchains.publicmasterpiece

import com.google.common.truth.Truth
import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryServiceFactory
import com.tangem.blockchain.assetsdiscovery.DefaultAssetsDiscoveryService
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.derivation.DerivationConfigV1
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.blockchain.common.derivation.DerivationConfigV3
import com.tangem.blockchain.common.isUTXO
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class PublicMasterpieceTest {

    private val blockchain = Blockchain.PublicMasterpiece

    @Test
    fun `Public Masterpiece has correct chain ID`() {
        Truth.assertThat(Chain.entries.find { it.blockchain == blockchain }?.id).isEqualTo(290290)
        Truth.assertThat(blockchain.getChainId()).isEqualTo(290290)
    }

    @Test
    fun `Public Masterpiece supports EIP-1559`() {
        Truth.assertThat(blockchain.isSupportEIP1559).isTrue()
    }

    @Test
    fun `Public Masterpiece is EVM and not UTXO blockchain`() {
        Truth.assertThat(blockchain.isEvm()).isTrue()
        Truth.assertThat(blockchain.isUTXO).isFalse()
    }

    @Test
    fun `Public Masterpiece does not handle tokens`() {
        Truth.assertThat(blockchain.canHandleTokens()).isFalse()
    }

    @Test
    fun `Public Masterpiece has correct id decimals currency and name`() {
        Truth.assertThat(blockchain.id).isEqualTo("public-masterpiece")
        Truth.assertThat(blockchain.decimals()).isEqualTo(18)
        Truth.assertThat(blockchain.currency).isEqualTo("PMT")
        Truth.assertThat(blockchain.fullName).isEqualTo("Public Masterpiece")
    }

    @Test
    fun `Public Masterpiece has no testnet`() {
        Truth.assertThat(blockchain.getTestnetVersion()).isNull()
        Truth.assertThat(blockchain.isTestnet()).isFalse()
    }

    @Test
    fun `Public Masterpiece supports Secp256k1 curve only`() {
        Truth.assertThat(blockchain.getSupportedCurves()).containsExactly(EllipticCurve.Secp256k1)
    }

    /**
     * The chain has no SLIP44 coin type, so V1 falls back to the Ethereum one like V2 and V3.
     */
    @Test
    fun `Public Masterpiece uses Ethereum derivation path in V1 V2 and V3`() {
        listOf(DerivationConfigV1, DerivationConfigV2, DerivationConfigV3).forEach { config ->
            Truth.assertThat(config.derivations(blockchain)[AddressType.Default]?.rawPath)
                .isEqualTo("m/44'/60'/0'/0/0")
        }
    }

    @Test
    fun `Public Masterpiece creates EthereumWalletManager`() {
        val walletManager = WalletManagerFactory(
            config = BlockchainSdkConfig(),
            blockchainProviderTypes = mapOf(blockchain to listOf(ProviderType.Public(url = MAINNET_URL))),
            blockchainDataStorage = object : BlockchainDataStorage {
                override suspend fun getOrNull(key: String): String? = null
                override suspend fun store(key: String, value: String) = Unit
                override suspend fun remove(key: String) = Unit
            },
            accountCreator = object : AccountCreator {
                override suspend fun createAccount(
                    blockchain: Blockchain,
                    walletPublicKey: ByteArray,
                ): Result<String> = Result.Success("account")
            },
            featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false),
        ).createLegacyWalletManager(
            blockchain = blockchain,
            walletPublicKey = TEST_PUBLIC_KEY.hexToBytes(),
            curve = EllipticCurve.Secp256k1,
        )

        Truth.assertThat(walletManager).isInstanceOf(EthereumWalletManager::class.java)
        Truth.assertThat(walletManager?.wallet?.blockchain).isEqualTo(blockchain)
    }

    @Test
    fun `Public Masterpiece has EVM assets discovery service`() {
        val service = AssetsDiscoveryServiceFactory(
            config = BlockchainSdkConfig(),
            providerTypes = mapOf(blockchain to listOf(ProviderType.Public(url = MAINNET_URL))),
        ).create(blockchain)

        Truth.assertThat(service).isNotEqualTo(DefaultAssetsDiscoveryService)
    }

    private companion object {
        const val MAINNET_URL = "https://node1.pmtchain.com/"
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}