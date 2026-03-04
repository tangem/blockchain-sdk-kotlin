package com.tangem.blockchain.blockchains.monad

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.Chain
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

/**
 * Tests for Monad blockchain support
 */
internal class MonadTest {

    @Test
    fun `Monad creates EthereumWalletManager`() {
        val walletManager = createMonadWalletManager(Blockchain.Monad)

        Truth.assertThat(walletManager).isInstanceOf(EthereumWalletManager::class.java)
        Truth.assertThat(walletManager?.wallet?.blockchain).isEqualTo(Blockchain.Monad)
    }

    @Test
    fun `MonadTestnet creates EthereumWalletManager`() {
        val walletManager = createMonadWalletManager(Blockchain.MonadTestnet)

        Truth.assertThat(walletManager).isInstanceOf(EthereumWalletManager::class.java)
        Truth.assertThat(walletManager?.wallet?.blockchain).isEqualTo(Blockchain.MonadTestnet)
    }

    @Test
    fun `Monad has correct chain ID`() {
        val monadChain = Chain.entries.find { it.blockchain == Blockchain.Monad }
        val monadTestnetChain = Chain.entries.find { it.blockchain == Blockchain.MonadTestnet }

        Truth.assertThat(monadChain?.id).isEqualTo(143)
        Truth.assertThat(monadTestnetChain?.id).isEqualTo(10143)
    }

    @Test
    fun `Monad supports EIP-1559`() {
        Truth.assertThat(Blockchain.Monad.isSupportEIP1559).isTrue()
        Truth.assertThat(Blockchain.MonadTestnet.isSupportEIP1559).isTrue()
    }

    @Test
    fun `Monad is EVM blockchain`() {
        Truth.assertThat(Blockchain.Monad.isEvm()).isTrue()
        Truth.assertThat(Blockchain.MonadTestnet.isEvm()).isTrue()
    }

    @Test
    fun `Monad is not UTXO blockchain`() {
        Truth.assertThat(Blockchain.Monad.isUTXO).isFalse()
        Truth.assertThat(Blockchain.MonadTestnet.isUTXO).isFalse()
    }

    @Test
    fun `Monad has correct decimals`() {
        Truth.assertThat(Blockchain.Monad.decimals()).isEqualTo(18)
        Truth.assertThat(Blockchain.MonadTestnet.decimals()).isEqualTo(18)
    }

    @Test
    fun `Monad has correct currency symbol`() {
        Truth.assertThat(Blockchain.Monad.currency).isEqualTo("MON")
        Truth.assertThat(Blockchain.MonadTestnet.currency).isEqualTo("MON")
    }

    @Test
    fun `Monad has correct network name`() {
        Truth.assertThat(Blockchain.Monad.fullName).isEqualTo("Monad")
        Truth.assertThat(Blockchain.MonadTestnet.fullName).isEqualTo("Monad Testnet")
    }

    @Test
    fun `Monad has correct testnet version`() {
        Truth.assertThat(Blockchain.Monad.getTestnetVersion()).isEqualTo(Blockchain.MonadTestnet)
        Truth.assertThat(Blockchain.MonadTestnet.getTestnetVersion()).isEqualTo(Blockchain.MonadTestnet)
    }

    @Test
    fun `MonadTestnet is testnet`() {
        Truth.assertThat(Blockchain.Monad.isTestnet()).isFalse()
        Truth.assertThat(Blockchain.MonadTestnet.isTestnet()).isTrue()
    }

    @Test
    fun `Monad supports Secp256k1 curve`() {
        val supportedCurves = Blockchain.Monad.getSupportedCurves()
        Truth.assertThat(supportedCurves).contains(EllipticCurve.Secp256k1)
    }

    @Test
    fun `Monad chain ID mapping is correct`() {
        val monadChainId = Blockchain.Monad.getChainId()
        val monadTestnetChainId = Blockchain.MonadTestnet.getChainId()

        Truth.assertThat(monadChainId).isEqualTo(143L)
        Truth.assertThat(monadTestnetChainId).isEqualTo(10143L)
    }

    private fun createMonadWalletManager(blockchain: Blockchain): WalletManager? {
        val publicKey = TEST_PUBLIC_KEY.hexToBytes()

        val config = BlockchainSdkConfig(
            infuraProjectId = "testProjectId",
            nowNodeCredentials = NowNodeCredentials(apiKey = "testApiKey"),
            getBlockCredentials = GetBlockCredentials(
                xrp = GetBlockAccessToken(),
                cardano = null,
                avalanche = null,
                eth = GetBlockAccessToken(),
                etc = null,
                fantom = null,
                rsk = null,
                bsc = null,
                polygon = null,
                gnosis = null,
                cronos = null,
                solana = null,
                ton = null,
                tron = null,
                cosmos = null,
                near = null,
                dogecoin = GetBlockAccessToken(),
                litecoin = GetBlockAccessToken(),
                dash = GetBlockAccessToken(),
                bitcoin = GetBlockAccessToken(),
                aptos = null,
                algorand = null,
                polygonZkEvm = null,
                zkSyncEra = null,
                base = null,
                blast = null,
                filecoin = null,
                arbitrum = null,
                bitcoinCash = null,
                kusama = null,
                moonbeam = null,
                optimism = null,
                polkadot = null,
                shibarium = null,
                sui = null,
                telos = null,
                tezos = null,
                monad = null,
                stellar = null,
            ),
        )

        return WalletManagerFactory(
            config = config,
            blockchainProviderTypes = mapOf(
                blockchain to listOf(ProviderType.Public(url = "https://rpc.monad.xyz/")),
            ),
            blockchainDataStorage = object : BlockchainDataStorage {
                override suspend fun getOrNull(key: String): String? = null
                override suspend fun store(key: String, value: String) = Unit
                override suspend fun remove(key: String) = Unit
            },
            accountCreator = object : AccountCreator {
                override suspend fun createAccount(blockchain: Blockchain, walletPublicKey: ByteArray): Result<String> {
                    return Result.Success("account")
                }
            },
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = false,
            ),
        ).createLegacyWalletManager(
            blockchain,
            publicKey,
            EllipticCurve.Secp256k1,
        )
    }

    private companion object {
        const val TEST_PUBLIC_KEY = "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321" +
            "998CC2D042901C91D40601E79A641E1CBCEBE7A2358BE6054E1B6E5D"
    }
}