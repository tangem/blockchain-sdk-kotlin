package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryServiceFactory
import com.tangem.blockchain.assetsdiscovery.providers.evm.DefaultEvmAssetsDiscoveryService
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
 * Tests for Arc blockchain support
 */
internal class ArcTest {

    @Test
    fun `Arc has correct chain ID`() {
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.Arc }?.id).isEqualTo(5042)
        Truth.assertThat(Chain.entries.find { it.blockchain == Blockchain.ArcTestnet }?.id).isEqualTo(5042002)
        Truth.assertThat(Blockchain.Arc.getChainId()).isEqualTo(5042)
        Truth.assertThat(Blockchain.ArcTestnet.getChainId()).isEqualTo(5042002)
    }

    @Test
    fun `Arc supports EIP-1559`() {
        Truth.assertThat(Blockchain.Arc.isSupportEIP1559).isTrue()
        Truth.assertThat(Blockchain.ArcTestnet.isSupportEIP1559).isTrue()
    }

    @Test
    fun `Arc is EVM and not UTXO blockchain`() {
        Truth.assertThat(Blockchain.Arc.isEvm()).isTrue()
        Truth.assertThat(Blockchain.ArcTestnet.isEvm()).isTrue()
        Truth.assertThat(Blockchain.Arc.isUTXO).isFalse()
        Truth.assertThat(Blockchain.ArcTestnet.isUTXO).isFalse()
    }

    @Test
    fun `Arc coin is named USDC, not after the network`() {
        Truth.assertThat(Blockchain.Arc.getCoinName()).isEqualTo("USDC")
        Truth.assertThat(Blockchain.ArcTestnet.getCoinName()).isEqualTo("USDC")
    }

    @Test
    fun `Arc operates with 18 decimals but displays USDC with 6`() {
        Truth.assertThat(Blockchain.Arc.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.ArcTestnet.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.Arc.displayDecimals()).isEqualTo(6)
        Truth.assertThat(Blockchain.ArcTestnet.displayDecimals()).isEqualTo(6)
    }

    @Test
    fun `display decimals equal decimals for every blockchain but Arc`() {
        val mismatched = Blockchain.entries.filter { it.displayDecimals() != it.decimals() }

        Truth.assertThat(mismatched).containsExactly(Blockchain.Arc, Blockchain.ArcTestnet)
    }

    @Test
    fun `Arc has correct currency and name`() {
        Truth.assertThat(Blockchain.Arc.currency).isEqualTo("USDC")
        Truth.assertThat(Blockchain.ArcTestnet.currency).isEqualTo("USDC")
        Truth.assertThat(Blockchain.Arc.fullName).isEqualTo("Arc")
        Truth.assertThat(Blockchain.ArcTestnet.fullName).isEqualTo("Arc Testnet")
        Truth.assertThat(Blockchain.Arc.id).isEqualTo("arc")
        Truth.assertThat(Blockchain.ArcTestnet.id).isEqualTo("arc/test")
    }

    @Test
    fun `Arc has correct testnet mapping`() {
        Truth.assertThat(Blockchain.Arc.getTestnetVersion()).isEqualTo(Blockchain.ArcTestnet)
        Truth.assertThat(Blockchain.ArcTestnet.getTestnetVersion()).isEqualTo(Blockchain.ArcTestnet)
        Truth.assertThat(Blockchain.Arc.isTestnet()).isFalse()
        Truth.assertThat(Blockchain.ArcTestnet.isTestnet()).isTrue()
    }

    @Test
    fun `Arc supports Secp256k1 curve only`() {
        Truth.assertThat(Blockchain.Arc.getSupportedCurves()).containsExactly(EllipticCurve.Secp256k1)
        Truth.assertThat(Blockchain.ArcTestnet.getSupportedCurves()).containsExactly(EllipticCurve.Secp256k1)
    }

    @Test
    fun `Arc handles tokens`() {
        Truth.assertThat(Blockchain.Arc.canHandleTokens()).isTrue()
        Truth.assertThat(Blockchain.ArcTestnet.canHandleTokens()).isTrue()
    }

    @Test
    fun `Arc uses its own coin type in V1 and Ethereum derivation path in V2 and V3`() {
        listOf(Blockchain.Arc, Blockchain.ArcTestnet).forEach { blockchain ->
            Truth.assertThat(
                DerivationConfigV1.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo("m/44'/5042'/0'/0/0")
            Truth.assertThat(
                DerivationConfigV2.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo("m/44'/60'/0'/0/0")
            Truth.assertThat(
                DerivationConfigV3.derivations(blockchain)[AddressType.Default]?.rawPath,
            ).isEqualTo("m/44'/60'/0'/0/0")
        }
    }

    @Test
    fun `Arc creates EthereumWalletManager`() {
        listOf(Blockchain.Arc, Blockchain.ArcTestnet).forEach { blockchain ->
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
    fun `Arc has EVM assets discovery service`() {
        listOf(Blockchain.Arc, Blockchain.ArcTestnet).forEach { blockchain ->
            val service = AssetsDiscoveryServiceFactory(
                config = BlockchainSdkConfig(),
                providerTypes = mapOf(
                    blockchain to listOf(ProviderType.Public(url = MAINNET_URL)),
                ),
            ).create(blockchain)

            Truth.assertThat(service).isInstanceOf(DefaultEvmAssetsDiscoveryService::class.java)
        }
    }

    private companion object {
        const val MAINNET_URL = "https://rpc.mainnet.arc.io/"
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}