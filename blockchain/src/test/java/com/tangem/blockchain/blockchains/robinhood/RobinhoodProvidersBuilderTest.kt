package com.tangem.blockchain.blockchains.robinhood

import android.util.Log
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.common.network.providers.getAllNonpublicProviderTypes
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test

internal class RobinhoodProvidersBuilderTest {

    @Before
    fun setup() {
        // android.util.Log is not available in JVM unit tests
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
    }

    @Test
    fun `blink provider is created when pending transactions are enabled`() {
        initToggles(isPendingTransactionsEnabled = true)

        val actual = RobinhoodProvidersBuilder(
            providerTypes = listOf(ProviderType.Blink),
            config = BlockchainSdkConfig(blinkApiKey = BLINK_API_KEY),
        ).build(Blockchain.Robinhood)

        Truth.assertThat(actual).hasSize(1)
        Truth.assertThat(actual.first().baseUrl).isEqualTo(BLINK_URL)
    }

    @Test
    fun `blink provider is skipped when pending transactions are disabled`() {
        initToggles(isPendingTransactionsEnabled = false)

        val actual = RobinhoodProvidersBuilder(
            providerTypes = listOf(ProviderType.Blink),
            config = BlockchainSdkConfig(blinkApiKey = BLINK_API_KEY),
        ).build(Blockchain.Robinhood)

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `blink provider is skipped when api key is blank`() {
        initToggles(isPendingTransactionsEnabled = true)

        val actual = RobinhoodProvidersBuilder(
            providerTypes = listOf(ProviderType.Blink),
            config = BlockchainSdkConfig(blinkApiKey = "  "),
        ).build(Blockchain.Robinhood)

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `providers keep the order of provider types`() {
        initToggles(isPendingTransactionsEnabled = true)

        val publicUrl = "https://rpc.mainnet.chain.robinhood.com/"

        val actual = RobinhoodProvidersBuilder(
            providerTypes = listOf(
                ProviderType.Blink,
                ProviderType.Public(url = publicUrl),
                ProviderType.QuickNode,
            ),
            config = BlockchainSdkConfig(
                blinkApiKey = BLINK_API_KEY,
                quickNodeRobinhoodCredentials = QuickNodeCredentials(
                    apiKey = "quicknode-api-key",
                    subdomain = "robinhood-mainnet.quiknode.pro",
                ),
            ),
        ).build(Blockchain.Robinhood)

        Truth.assertThat(actual.map { it.baseUrl })
            .containsExactly(
                BLINK_URL,
                publicUrl,
                "https://robinhood-mainnet.quiknode.pro/quicknode-api-key/",
            )
            .inOrder()
    }

    @Test
    fun `unsupported provider types are ignored`() {
        initToggles(isPendingTransactionsEnabled = true)

        val unsupportedTypes = getAllNonpublicProviderTypes() - setOf(ProviderType.Blink, ProviderType.QuickNode)

        val actual = RobinhoodProvidersBuilder(
            providerTypes = unsupportedTypes,
            config = BlockchainSdkConfig(blinkApiKey = BLINK_API_KEY),
        ).build(Blockchain.Robinhood)

        Truth.assertThat(actual).isEmpty()
    }

    private fun initToggles(isPendingTransactionsEnabled: Boolean) {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = false,
                isPendingTransactionsEnabled = isPendingTransactionsEnabled,
            ),
        )
    }

    private companion object {
        const val BLINK_URL = "https://robinhood.blinklabs.xyz/v1/"
        const val BLINK_API_KEY = "blink-api-key"
    }
}