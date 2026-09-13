package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.blockchain.common.network.providers.ProviderType
import org.junit.Test

/**
 * Tests for ArcProvidersBuilder
 */
internal class ArcProvidersBuilderTest {

    @Test
    fun `ArcProvidersBuilder creates mainnet providers from public types`() {
        val builder = ArcProvidersBuilder(
            providerTypes = listOf(ProviderType.Public(url = MAINNET_URL)),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Arc)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(MAINNET_URL)
    }

    @Test
    fun `ArcProvidersBuilder creates QuickNode and Alchemy mainnet providers`() {
        val builder = ArcProvidersBuilder(
            providerTypes = listOf(ProviderType.QuickNode, ProviderType.Alchemy),
            config = BlockchainSdkConfig(
                quickNodeArcCredentials = QuickNodeCredentials(apiKey = "key", subdomain = "quiknodeArc"),
                alchemyApiKey = "alchemyKey",
            ),
        )

        val providers = builder.build(Blockchain.Arc)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(
            "https://quiknodeArc/key/",
            "https://arc-mainnet.g.alchemy.com/v2/alchemyKey/",
        ).inOrder()
    }

    @Test
    fun `ArcProvidersBuilder skips QuickNode and Alchemy without credentials`() {
        val builder = ArcProvidersBuilder(
            providerTypes = listOf(ProviderType.QuickNode, ProviderType.Alchemy, ProviderType.Public(MAINNET_URL)),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Arc)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(MAINNET_URL)
    }

    @Test
    fun `ArcProvidersBuilder ignores unsupported provider types`() {
        val builder = ArcProvidersBuilder(
            providerTypes = listOf(ProviderType.NowNodes, ProviderType.GetBlock, ProviderType.Public(MAINNET_URL)),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.Arc)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(MAINNET_URL)
    }

    @Test
    fun `ArcProvidersBuilder creates testnet providers`() {
        val builder = ArcProvidersBuilder(
            providerTypes = emptyList(),
            config = BlockchainSdkConfig(alchemyApiKey = "alchemyKey"),
        )

        val providers = builder.build(Blockchain.ArcTestnet)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(
            "https://rpc.testnet.arc.io/",
            "https://arc-testnet.g.alchemy.com/v2/alchemyKey/",
        ).inOrder()
    }

    private companion object {
        const val MAINNET_URL = "https://rpc.mainnet.arc.io/"
    }
}