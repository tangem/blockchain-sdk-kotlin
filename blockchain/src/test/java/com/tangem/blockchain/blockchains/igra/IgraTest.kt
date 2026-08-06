package com.tangem.blockchain.blockchains.igra

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
 * Tests for Igra blockchain support
 */
internal class IgraTest {

    @Test
    fun `Igra has correct chain ID`() {
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.Igra }?.id).isEqualTo(38833)
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.IgraTestnet }?.id).isEqualTo(38836)
        Truth.assertThat(Blockchain.Igra.getChainId()).isEqualTo(38833)
        Truth.assertThat(Blockchain.IgraTestnet.getChainId()).isEqualTo(38836)
    }

    @Test
    fun `Igra supports EIP-1559`() {
        Truth.assertThat(Blockchain.Igra.isSupportEIP1559).isTrue()
        Truth.assertThat(Blockchain.IgraTestnet.isSupportEIP1559).isTrue()
    }

    @Test
    fun `Igra is EVM and not UTXO blockchain`() {
        Truth.assertThat(Blockchain.Igra.isEvm()).isTrue()
        Truth.assertThat(Blockchain.IgraTestnet.isEvm()).isTrue()
        Truth.assertThat(Blockchain.Igra.isUTXO).isFalse()
        Truth.assertThat(Blockchain.IgraTestnet.isUTXO).isFalse()
    }

    @Test
    fun `Igra has correct decimals currency and name`() {
        Truth.assertThat(Blockchain.Igra.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.IgraTestnet.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.Igra.currency).isEqualTo("iKAS")
        Truth.assertThat(Blockchain.IgraTestnet.currency).isEqualTo("iKAS")
        Truth.assertThat(Blockchain.Igra.fullName).isEqualTo("Igra")
        Truth.assertThat(Blockchain.IgraTestnet.fullName).isEqualTo("Igra Testnet")
    }

    @Test
    fun `Igra has correct testnet mapping`() {
        Truth.assertThat(Blockchain.Igra.getTestnetVersion()).isEqualTo(Blockchain.IgraTestnet)
        Truth.assertThat(Blockchain.IgraTestnet.getTestnetVersion()).isEqualTo(Blockchain.IgraTestnet)
        Truth.assertThat(Blockchain.Igra.isTestnet()).isFalse()
        Truth.assertThat(Blockchain.IgraTestnet.isTestnet()).isTrue()
    }

    @Test
    fun `Igra supports Secp256k1 curve only`() {
        Truth.assertThat(Blockchain.Igra.getSupportedCurves()).containsExactly(EllipticCurve.Secp256k1)
        Truth.assertThat(Blockchain.IgraTestnet.getSupportedCurves())
            .containsExactly(EllipticCurve.Secp256k1)
    }

    @Test
    fun `Igra uses Ethereum derivation path in all configs`() {
        val expected = "m/44'/60'/0'/0/0"

        listOf(Blockchain.Igra, Blockchain.IgraTestnet).forEach { blockchain ->
            Truth.assertThat(
                DerivationConfigV1.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo(expected)
            Truth.assertThat(
                DerivationConfigV2.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo(expected)
            Truth.assertThat(
                DerivationConfigV3.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo(expected)
        }
    }

    @Test
    fun `Igra creates EthereumWalletManager`() {
        listOf(Blockchain.Igra, Blockchain.IgraTestnet).forEach { blockchain ->
            val walletManager = WalletManagerFactory(
                config = BlockchainSdkConfig(),
                blockchainProviderTypes = mapOf(
                    blockchain to listOf(ProviderType.Public(url = "https://rpc.igralabs.com:8545/")),
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
    fun `Igra has EVM assets discovery service`() {
        listOf(Blockchain.Igra, Blockchain.IgraTestnet).forEach { blockchain ->
            val service = AssetsDiscoveryServiceFactory(
                config = BlockchainSdkConfig(),
                providerTypes = mapOf(
                    blockchain to listOf(ProviderType.Public(url = "https://rpc.igralabs.com:8545/")),
                ),
            ).create(blockchain)

            Truth.assertThat(service).isNotEqualTo(DefaultAssetsDiscoveryService)
        }
    }

    private companion object {
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}