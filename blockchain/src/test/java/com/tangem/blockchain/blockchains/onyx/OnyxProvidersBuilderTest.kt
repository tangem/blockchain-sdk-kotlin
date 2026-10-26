package com.tangem.blockchain.blockchains.onyx

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType
import org.junit.Test

internal class OnyxProvidersBuilderTest {

    @Test
    fun `OnyxProvidersBuilder creates mainnet providers from public types`() {
        val builder = OnyxProvidersBuilder(
            providerTypes = listOf(
                ProviderType.Public(url = "https://rpc.onyx.org/"),
                ProviderType.Public(url = "https://rpc-onyx.chain.com/"),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Onyx)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(
            "https://rpc.onyx.org/",
            "https://rpc-onyx.chain.com/",
        )
    }

    @Test
    fun `OnyxProvidersBuilder creates testnet provider`() {
        val builder = OnyxProvidersBuilder(
            providerTypes = emptyList(),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.OnyxTestnet)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly("https://rpc.testnet.goliath.net/")
    }

    @Test
    fun `OnyxProvidersBuilder ignores unsupported provider types`() {
        val validUrl = "https://rpc.onyx.org/"
        val builder = OnyxProvidersBuilder(
            providerTypes = listOf(
                ProviderType.NowNodes,
                ProviderType.QuickNode,
                ProviderType.Public(url = validUrl),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Onyx)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(validUrl)
    }
}