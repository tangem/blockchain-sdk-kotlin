package com.tangem.blockchain.blockchains.igra

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType
import org.junit.Test

/**
 * Tests for IgraProvidersBuilder
 */
internal class IgraProvidersBuilderTest {

    @Test
    fun `IgraProvidersBuilder creates mainnet providers from public types`() {
        val builder = IgraProvidersBuilder(
            providerTypes = listOf(
                ProviderType.Public(url = "https://rpc.igralabs.com:8545/"),
                ProviderType.Public(url = "https://igra-mainnet.jobberwocky.co/"),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Igra)

        val urls = providers.map { it.baseUrl }
        Truth.assertThat(urls).containsExactly(
            "https://rpc.igralabs.com:8545/",
            "https://igra-mainnet.jobberwocky.co/",
        )
    }

    @Test
    fun `IgraProvidersBuilder creates testnet provider`() {
        val builder = IgraProvidersBuilder(
            providerTypes = emptyList(),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.IgraTestnet)

        Truth.assertThat(providers.map { it.baseUrl })
            .containsExactly("https://galleon-testnet.igralabs.com:8545")
    }

    @Test
    fun `IgraProvidersBuilder ignores unsupported provider types`() {
        val validUrl = "https://rpc.igralabs.com:8545/"
        val builder = IgraProvidersBuilder(
            providerTypes = listOf(
                ProviderType.NowNodes,
                ProviderType.QuickNode,
                ProviderType.Public(url = validUrl),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Igra)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(validUrl)
    }
}