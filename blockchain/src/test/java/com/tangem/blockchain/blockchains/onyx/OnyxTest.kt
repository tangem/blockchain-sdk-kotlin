package com.tangem.blockchain.blockchains.onyx

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

internal class OnyxTest {

    @Test
    fun `Onyx has correct chain ID`() {
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.Onyx }?.id).isEqualTo(327)
        Truth.assertThat(Blockchain.Onyx.getChainId()).isEqualTo(327)
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.OnyxTestnet }?.id).isEqualTo(8901)
        Truth.assertThat(Blockchain.OnyxTestnet.getChainId()).isEqualTo(8901)
    }

    @Test
    fun `Onyx does not support EIP-1559`() {
        Truth.assertThat(Blockchain.Onyx.isSupportEIP1559).isFalse()
    }

    @Test
    fun `Onyx is EVM and not UTXO blockchain`() {
        Truth.assertThat(Blockchain.Onyx.isEvm()).isTrue()
        Truth.assertThat(Blockchain.Onyx.isUTXO).isFalse()
    }

    @Test
    fun `Onyx has correct decimals currency and name`() {
        Truth.assertThat(Blockchain.Onyx.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.Onyx.displayDecimals()).isEqualTo(8)
        Truth.assertThat(Blockchain.Onyx.currency).isEqualTo("XCN")
        Truth.assertThat(Blockchain.Onyx.id).isEqualTo("onyx")
        Truth.assertThat(Blockchain.Onyx.fullName).isEqualTo("Onyx")
    }

    @Test
    fun `Onyx has correct testnet mapping`() {
        Truth.assertThat(Blockchain.Onyx.getTestnetVersion()).isEqualTo(Blockchain.OnyxTestnet)
        Truth.assertThat(Blockchain.OnyxTestnet.getTestnetVersion()).isEqualTo(Blockchain.OnyxTestnet)
        Truth.assertThat(Blockchain.Onyx.isTestnet()).isFalse()
        Truth.assertThat(Blockchain.OnyxTestnet.isTestnet()).isTrue()
        Truth.assertThat(Blockchain.OnyxTestnet.id).isEqualTo("onyx/test")
        Truth.assertThat(Blockchain.OnyxTestnet.displayDecimals()).isEqualTo(8)
        Truth.assertThat(Blockchain.OnyxTestnet.isEvm()).isTrue()
        Truth.assertThat(Blockchain.OnyxTestnet.isSupportEIP1559).isFalse()
    }

    @Test
    fun `Onyx supports Secp256k1 curve only`() {
        Truth.assertThat(Blockchain.Onyx.getSupportedCurves()).containsExactly(EllipticCurve.Secp256k1)
    }

    /** Onyx has no SLIP44 coin type, so V1 falls back to the Ethereum one like V2 and V3. */
    @Test
    fun `Onyx uses Ethereum derivation path in V1 V2 and V3`() {
        listOf(Blockchain.Onyx, Blockchain.OnyxTestnet).forEach { blockchain ->
            listOf(DerivationConfigV1, DerivationConfigV2, DerivationConfigV3).forEach { config ->
                Truth.assertThat(config.derivations(blockchain)[AddressType.Default]?.rawPath)
                    .isEqualTo("m/44'/60'/0'/0/0")
            }
        }
    }

    @Test
    fun `Onyx creates EthereumWalletManager`() {
        listOf(Blockchain.Onyx, Blockchain.OnyxTestnet).forEach(::assertCreatesEthereumWalletManager)
    }

    private fun assertCreatesEthereumWalletManager(blockchain: Blockchain) {
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
    fun `Onyx has EVM assets discovery service`() {
        listOf(Blockchain.Onyx, Blockchain.OnyxTestnet).forEach { blockchain ->
            val service = AssetsDiscoveryServiceFactory(
                config = BlockchainSdkConfig(),
                providerTypes = mapOf(blockchain to listOf(ProviderType.Public(url = MAINNET_URL))),
            ).create(blockchain)

            Truth.assertThat(service).isNotEqualTo(DefaultAssetsDiscoveryService)
        }
    }

    private companion object {
        const val MAINNET_URL = "https://rpc.onyx.org/"
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}