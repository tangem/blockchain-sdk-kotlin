package com.tangem.blockchain.common.network.providers

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.providers.*
import com.tangem.blockchain.blockchains.filecoin.FilecoinProvidersBuilder
import com.tangem.blockchain.blockchains.telos.TelosProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.PublicProvidersWithPostfixNetworkBuilderTest.Companion.data
import com.tangem.blockchain.common.network.providers.PublicProvidersWithPostfixNetworkBuilderTest.Model
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Parameterized testing public providers that required absent slash in the end of base url.
 * Example: https://base-url/postfix-url        – isn't working
 *          https://base-url/   +   postfix-url – is working
 *
 * 1. Add a new method creating [Model].
 * 2. Add new Test's inputs in [data] method.
 *
 * @see [AvalancheProvidersBuilder], [ArbitrumProvidersBuilder], etc.
 */
@RunWith(Parameterized::class)
internal class PublicProvidersWithPostfixNetworkBuilderTest(private val model: Model) {

    @Test
    fun test() {
        val providerTypes = listOf(ProviderType.Public(url = model.baseUrl))

        val actual = model.builder(providerTypes).build(model.blockchain).first().baseUrl

        Truth.assertThat(actual).isEqualTo(model.expectedUrl)
    }

    data class Model(
        val baseUrl: String,
        val expectedUrl: String,
        val blockchain: Blockchain,
        val builder: (providerTypes: List<ProviderType>) -> NetworkProvidersBuilder<*>,
    )

    private companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            createArbitrum(baseUrl = "https://arb.com/", expectedUrl = "https://arb.com/"),
            createArbitrum(baseUrl = "https://arb.com/arb/", expectedUrl = "https://arb.com/"),

            createAurora(baseUrl = "https://aurora.com/", expectedUrl = "https://aurora.com/"),
            createAurora(baseUrl = "https://aurora.com/aurora/", expectedUrl = "https://aurora.com/"),

            createAvalanche(baseUrl = "https://avax.com/", expectedUrl = "https://avax.com/"),
            createAvalanche(baseUrl = "https://avax.com/ext/bc/C/rpc/", expectedUrl = "https://avax.com/"),

            createFlare(baseUrl = "https://flare.com/", expectedUrl = "https://flare.com/"),
            createFlare(baseUrl = "https://flare.com/ext/C/rpc/", expectedUrl = "https://flare.com/"),
            createFlare(baseUrl = "https://flare.com/ext/bc/C/rpc/", expectedUrl = "https://flare.com/"),

            createMoonbeam(baseUrl = "https://moonbeam.com/", expectedUrl = "https://moonbeam.com/"),
            createMoonbeam(baseUrl = "https://moonbeam.com/glmr/", expectedUrl = "https://moonbeam.com/"),

            createPolygonZkEVM(baseUrl = "https://zkevm.com/", expectedUrl = "https://zkevm.com/"),
            createPolygonZkEVM(baseUrl = "https://zkevm.com/zkevm/", expectedUrl = "https://zkevm.com/"),

            createTelos(baseUrl = "https://telos.com/", expectedUrl = "https://telos.com/"),
            createTelos(baseUrl = "https://telos.com/evm/", expectedUrl = "https://telos.com/"),

            createZkSyncEra(baseUrl = "https://zksync.com/", expectedUrl = "https://zksync.com/"),
            createZkSyncEra(baseUrl = "https://zksync.com/zksync2-era/", expectedUrl = "https://zksync.com/"),

            createFilecoin(baseUrl = "https://fil.io/", expectedUrl = "https://fil.io/"),
            createFilecoin(baseUrl = "https://fil.io/rpc/v1/", expectedUrl = "https://fil.io/"),
        )

        private fun createArbitrum(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Arbitrum,
                builder = { ArbitrumProvidersBuilder(providerTypes = it, config = BlockchainSdkConfig()) },
            )
        }

        private fun createAurora(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Aurora,
                builder = ::AuroraProvidersBuilder,
            )
        }

        private fun createAvalanche(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Avalanche,
                builder = { AvalancheProvidersBuilder(providerTypes = it, config = BlockchainSdkConfig()) },
            )
        }

        private fun createFlare(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Flare,
                builder = ::FlareProvidersBuilder,
            )
        }

        private fun createMoonbeam(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Moonbeam,
                builder = { MoonbeamProvidersBuilder(providerTypes = it, config = BlockchainSdkConfig()) },
            )
        }

        private fun createPolygonZkEVM(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.PolygonZkEVM,
                builder = { PolygonZkEVMProvidersBuilder(providerTypes = it, config = BlockchainSdkConfig()) },
            )
        }

        private fun createTelos(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Telos,
                builder = ::TelosProvidersBuilder,
            )
        }

        private fun createZkSyncEra(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.ZkSyncEra,
                builder = { ZkSyncEraProvidersBuilder(providerTypes = it, config = BlockchainSdkConfig()) },
            )
        }

        private fun createFilecoin(baseUrl: String, expectedUrl: String): Model {
            return Model(
                baseUrl = baseUrl,
                expectedUrl = expectedUrl,
                blockchain = Blockchain.Filecoin,
                builder = { FilecoinProvidersBuilder(providerTypes = it, config = BlockchainSdkConfig()) },
            )
        }
    }
}