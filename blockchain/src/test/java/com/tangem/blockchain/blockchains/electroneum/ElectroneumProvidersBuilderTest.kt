package com.tangem.blockchain.blockchains.electroneum

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType
import org.junit.Test

/**
 * Tests for ElectroneumProvidersBuilder
 */
internal class ElectroneumProvidersBuilderTest {

    @Test
    fun `ElectroneumProvidersBuilder creates mainnet providers from public types`() {
        val builder = ElectroneumProvidersBuilder(
            providerTypes = listOf(
                ProviderType.Public(url = "https://rpc.ankr.com/electroneum/"),
                ProviderType.Public(url = "https://rpc.electroneum.com/"),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Electroneum)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(
            "https://rpc.ankr.com/electroneum/",
            "https://rpc.electroneum.com/",
        )
    }

    @Test
    fun `ElectroneumProvidersBuilder creates testnet provider`() {
        val builder = ElectroneumProvidersBuilder(
            providerTypes = emptyList(),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.ElectroneumTestnet)

        Truth.assertThat(providers.map { it.baseUrl })
            .containsExactly("https://rpc.ankr.com/electroneum_testnet/")
    }

    @Test
    fun `ElectroneumProvidersBuilder ignores unsupported provider types`() {
        val validUrl = "https://rpc.electroneum.com/"
        val builder = ElectroneumProvidersBuilder(
            providerTypes = listOf(
                ProviderType.NowNodes,
                ProviderType.QuickNode,
                ProviderType.Public(url = validUrl),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Electroneum)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(validUrl)
    }
}