package com.tangem.blockchain.blockchains.publicmasterpiece

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType
import org.junit.Test

internal class PublicMasterpieceProvidersBuilderTest {

    @Test
    fun `PublicMasterpieceProvidersBuilder creates providers from public types`() {
        val builder = PublicMasterpieceProvidersBuilder(
            providerTypes = listOf(
                ProviderType.Public(url = "https://node1.pmtchain.com/"),
                ProviderType.Public(url = "https://node2.pmtchain.com/"),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.PublicMasterpiece)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(
            "https://node1.pmtchain.com/",
            "https://node2.pmtchain.com/",
        )
    }

    @Test
    fun `PublicMasterpieceProvidersBuilder ignores unsupported provider types`() {
        val validUrl = "https://node1.pmtchain.com/"
        val builder = PublicMasterpieceProvidersBuilder(
            providerTypes = listOf(
                ProviderType.NowNodes,
                ProviderType.QuickNode,
                ProviderType.Public(url = validUrl),
            ),
            config = BlockchainSdkConfig(),
        )

        val providers = builder.build(Blockchain.PublicMasterpiece)

        Truth.assertThat(providers.map { it.baseUrl }).containsExactly(validUrl)
    }
}