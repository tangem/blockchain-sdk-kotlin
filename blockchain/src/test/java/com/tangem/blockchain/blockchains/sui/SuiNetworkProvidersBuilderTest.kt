package com.tangem.blockchain.blockchains.sui

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.GetBlockAccessToken
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.network.providers.ProviderType
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class SuiNetworkProvidersBuilderTest {

    private val publicUrl = "https://fullnode.mainnet.sui.io/"
    private val providerTypes = listOf(
        ProviderType.Public(url = publicUrl),
        ProviderType.GetBlock,
        ProviderType.NowNodes,
        ProviderType.QuickNode,
    )

    init {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = false),
        )
    }

    @Test
    fun `GIVEN all credentials WHEN build THEN creates gRPC providers in configured order`() {
        // Arrange
        val getBlockCredentials = mockk<GetBlockCredentials> {
            every { sui } returns GetBlockAccessToken(jsonRpc = "jsonRpc", grpc = "grpc")
        }
        val config = BlockchainSdkConfig(
            getBlockCredentials = getBlockCredentials,
            nowNodeCredentials = NowNodeCredentials(apiKey = "nowNodes"),
            quickNodeSuiCredentials = QuickNodeCredentials(
                apiKey = "quickNode",
                subdomain = "name.sui-mainnet.quiknode.pro",
            ),
        )

        // Act
        val actual = SuiNetworkProvidersBuilder(providerTypes, config).build(Blockchain.Sui)

        // Assert
        Truth.assertThat(actual.map { it.baseUrl })
            .containsExactly(
                publicUrl,
                "https://shared.us-east-1.getblock.io/",
                "https://sui-grpc.nownodes.io/",
                "https://name.sui-mainnet.quiknode.pro/",
            )
            .inOrder()
    }

    @Test
    fun `GIVEN only JSON-RPC GetBlock token and no QuickNode credentials WHEN build THEN skips those providers`() {
        // Arrange
        val getBlockCredentials = mockk<GetBlockCredentials> {
            every { sui } returns GetBlockAccessToken(jsonRpc = "jsonRpc")
        }
        val config = BlockchainSdkConfig(
            getBlockCredentials = getBlockCredentials,
            nowNodeCredentials = NowNodeCredentials(apiKey = "nowNodes"),
            quickNodeSuiCredentials = QuickNodeCredentials(apiKey = "", subdomain = ""),
        )

        // Act
        val actual = SuiNetworkProvidersBuilder(providerTypes, config).build(Blockchain.Sui)

        // Assert
        Truth.assertThat(actual.map { it.baseUrl })
            .containsExactly(publicUrl, "https://sui-grpc.nownodes.io/")
            .inOrder()
    }
}