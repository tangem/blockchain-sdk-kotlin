package com.tangem.blockchain.blockchains.monad

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.providers.MonadProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.blockchain.common.network.providers.ProviderType
import org.junit.Test

/**
 * Tests for MonadProvidersBuilder
 */
internal class MonadProvidersBuilderTest {

    @Test
    fun `MonadProvidersBuilder creates mainnet providers`() {
        val builder = MonadProvidersBuilder(
            providerTypes = listOf(
                ProviderType.Public(url = "https://rpc.monad.xyz/"),
                ProviderType.Public(url = "https://rpc3.monad.xyz/"),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Monad)

        Truth.assertThat(providers).isNotEmpty()
        Truth.assertThat(providers.size).isEqualTo(2)

        val urls = providers.map { it.baseUrl }
        Truth.assertThat(urls).contains("https://rpc.monad.xyz/")
        Truth.assertThat(urls).contains("https://rpc3.monad.xyz/")
    }

    @Test
    fun `MonadProvidersBuilder creates testnet providers`() {
        val builder = MonadProvidersBuilder(
            providerTypes = emptyList(),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.MonadTestnet)

        Truth.assertThat(providers).isNotEmpty()
        Truth.assertThat(providers.size).isEqualTo(1)
        Truth.assertThat(providers.first().baseUrl).isEqualTo("https://testnet-rpc.monad.xyz/")
    }

    @Test
    fun `MonadProvidersBuilder includes public provider if specified`() {
        val customUrl = "https://custom-monad-rpc.com/"
        val builder = MonadProvidersBuilder(
            providerTypes = listOf(ProviderType.Public(url = customUrl)),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Monad)

        Truth.assertThat(providers.size).isEqualTo(1)
        Truth.assertThat(providers.first().baseUrl).isEqualTo(customUrl)
    }

    @Test
    fun `MonadProvidersBuilder creates QuickNode provider with credentials`() {
        val builder = MonadProvidersBuilder(
            providerTypes = listOf(ProviderType.QuickNode),
            config = BlockchainSdkConfig(
                quickNodeMonadCredentials = QuickNodeCredentials(
                    apiKey = "test-api-key",
                    subdomain = "monad-mainnet.quiknode.pro",
                ),
            ),
        )

        val providers = builder.build(Blockchain.Monad)

        Truth.assertThat(providers.size).isEqualTo(1)
        Truth.assertThat(providers.first().baseUrl).isEqualTo("https://monad-mainnet.quiknode.pro/test-api-key/")
    }

    @Test
    fun `MonadProvidersBuilder ignores unsupported provider types`() {
        val validUrl = "https://rpc.monad.xyz/"
        val builder = MonadProvidersBuilder(
            providerTypes = listOf(
                ProviderType.NowNodes,
                ProviderType.Public(url = validUrl),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Monad)

        Truth.assertThat(providers.size).isEqualTo(1)
        Truth.assertThat(providers.first().baseUrl).isEqualTo(validUrl)
    }
}