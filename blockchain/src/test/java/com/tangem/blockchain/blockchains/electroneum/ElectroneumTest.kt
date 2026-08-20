package com.tangem.blockchain.blockchains.electroneum

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

/**
 * Tests for Electroneum blockchain support
 */
internal class ElectroneumTest {

    @Test
    fun `Electroneum has correct chain ID`() {
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.Electroneum }?.id).isEqualTo(52014)
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.ElectroneumTestnet }?.id)
            .isEqualTo(5201420)
        Truth.assertThat(Blockchain.Electroneum.getChainId()).isEqualTo(52014)
        Truth.assertThat(Blockchain.ElectroneumTestnet.getChainId()).isEqualTo(5201420)
    }

    @Test
    fun `Electroneum supports EIP-1559`() {
        Truth.assertThat(Blockchain.Electroneum.isSupportEIP1559).isTrue()
        Truth.assertThat(Blockchain.ElectroneumTestnet.isSupportEIP1559).isTrue()
    }

    @Test
    fun `Electroneum is EVM and not UTXO blockchain`() {
        Truth.assertThat(Blockchain.Electroneum.isEvm()).isTrue()
        Truth.assertThat(Blockchain.ElectroneumTestnet.isEvm()).isTrue()
        Truth.assertThat(Blockchain.Electroneum.isUTXO).isFalse()
        Truth.assertThat(Blockchain.ElectroneumTestnet.isUTXO).isFalse()
    }

    @Test
    fun `Electroneum has correct decimals currency and name`() {
        Truth.assertThat(Blockchain.Electroneum.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.ElectroneumTestnet.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.Electroneum.currency).isEqualTo("ETN")
        Truth.assertThat(Blockchain.ElectroneumTestnet.currency).isEqualTo("ETN")
        Truth.assertThat(Blockchain.Electroneum.fullName).isEqualTo("Electroneum")
        Truth.assertThat(Blockchain.ElectroneumTestnet.fullName).isEqualTo("Electroneum Testnet")
    }

    @Test
    fun `Electroneum has correct testnet mapping`() {
        Truth.assertThat(Blockchain.Electroneum.getTestnetVersion()).isEqualTo(Blockchain.ElectroneumTestnet)
        Truth.assertThat(Blockchain.ElectroneumTestnet.getTestnetVersion())
            .isEqualTo(Blockchain.ElectroneumTestnet)
        Truth.assertThat(Blockchain.Electroneum.isTestnet()).isFalse()
        Truth.assertThat(Blockchain.ElectroneumTestnet.isTestnet()).isTrue()
    }

    @Test
    fun `Electroneum supports Secp256k1 curve only`() {
        Truth.assertThat(Blockchain.Electroneum.getSupportedCurves())
            .containsExactly(EllipticCurve.Secp256k1)
        Truth.assertThat(Blockchain.ElectroneumTestnet.getSupportedCurves())
            .containsExactly(EllipticCurve.Secp256k1)
    }

    /**
     * V1 uses the Electroneum SLIP44 coin type (415); V2 and V3 use the shared Ethereum path.
     */
    @Test
    fun `Electroneum uses its own derivation path in V1 and Ethereum one in V2 and V3`() {
        listOf(Blockchain.Electroneum, Blockchain.ElectroneumTestnet).forEach { blockchain ->
            Truth.assertThat(
                DerivationConfigV1.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo("m/44'/415'/0'/0/0")
            Truth.assertThat(
                DerivationConfigV2.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo("m/44'/60'/0'/0/0")
            Truth.assertThat(
                DerivationConfigV3.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo("m/44'/60'/0'/0/0")
        }
    }

    @Test
    fun `Electroneum creates EthereumWalletManager`() {
        listOf(Blockchain.Electroneum, Blockchain.ElectroneumTestnet).forEach { blockchain ->
            val walletManager = WalletManagerFactory(
                config = BlockchainSdkConfig(),
                blockchainProviderTypes = mapOf(
                    blockchain to listOf(ProviderType.Public(url = MAINNET_URL)),
                ),
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
    }

    @Test
    fun `Electroneum has EVM assets discovery service`() {
        listOf(Blockchain.Electroneum, Blockchain.ElectroneumTestnet).forEach { blockchain ->
            val service = AssetsDiscoveryServiceFactory(
                config = BlockchainSdkConfig(),
                providerTypes = mapOf(
                    blockchain to listOf(ProviderType.Public(url = MAINNET_URL)),
                ),
            ).create(blockchain)

            Truth.assertThat(service).isNotEqualTo(DefaultAssetsDiscoveryService)
        }
    }

    private companion object {
        const val MAINNET_URL = "https://rpc.electroneum.com/"
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}