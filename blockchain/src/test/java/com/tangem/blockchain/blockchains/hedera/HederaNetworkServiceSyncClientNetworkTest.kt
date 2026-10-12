package com.tangem.blockchain.blockchains.hedera

import com.google.common.truth.Truth.assertThat
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkNodeResponse
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.blockchains.hedera.network.HederaServiceEndpointResponse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HederaNetworkServiceSyncClientNetworkTest {

    private val client = Client.forMainnet()

    @Before
    fun setUp() {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = false,
                isHederaErc20Enabled = true,
            ),
        )
    }

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun syncClientNetwork_replacesClientNetworkWithPlaintextEndpoints() = runTest {
        val service = serviceWith(
            providerReturning(
                node(
                    nodeId = 0,
                    accountId = "0.0.3",
                    endpoint(ip = "34.64.136.190", port = 50211),
                    endpoint(ip = "34.64.136.190", port = 50212),
                    endpoint(ip = "43.202.249.148", port = 50211),
                ),
                node(
                    nodeId = 30,
                    accountId = "0.0.33",
                    endpoint(domain = "node33.hedera.example", ip = "1.2.3.4", port = 50211),
                ),
            ),
        )

        val result = service.syncClientNetwork()

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(client.network).containsExactly(
            "34.64.136.190:50211",
            AccountId.fromString("0.0.3"),
            "43.202.249.148:50211",
            AccountId.fromString("0.0.3"),
            "node33.hedera.example:50211",
            AccountId.fromString("0.0.33"),
        )
    }

    @Test
    fun syncClientNetwork_keepsClientNetwork_whenProviderFails() = runTest {
        val provider = mockk<HederaNetworkProvider>()
        every { provider.baseUrl } returns BASE_URL
        coEvery { provider.getNetworkNodes() } returns Result.Failure(
            BlockchainSdkError.WrappedThrowable(IOException("boom")),
        )
        val bundledNetwork = client.network.toMap()

        val result = serviceWith(provider).syncClientNetwork()

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat(client.network).isEqualTo(bundledNetwork)
    }

    @Test
    fun syncClientNetwork_keepsClientNetwork_whenNodesHaveNoPlaintextEndpoints() = runTest {
        val service = serviceWith(
            providerReturning(node(nodeId = 0, accountId = "0.0.3", endpoint(ip = "34.64.136.190", port = 50212))),
        )
        val bundledNetwork = client.network.toMap()

        val result = service.syncClientNetwork()

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat(client.network).isEqualTo(bundledNetwork)
    }

    private fun serviceWith(provider: HederaNetworkProvider): HederaNetworkService {
        return HederaNetworkService(
            client = client,
            hederaNetworkProviders = listOf(provider),
            blockchain = Blockchain.Hedera,
        )
    }

    private fun providerReturning(vararg nodes: HederaNetworkNodeResponse): HederaNetworkProvider {
        val provider = mockk<HederaNetworkProvider>()
        every { provider.baseUrl } returns BASE_URL
        coEvery { provider.getNetworkNodes() } returns Result.Success(nodes.toList())
        return provider
    }

    private fun node(
        nodeId: Long,
        accountId: String,
        vararg endpoints: HederaServiceEndpointResponse,
    ): HederaNetworkNodeResponse {
        return HederaNetworkNodeResponse(
            nodeId = nodeId,
            nodeAccountId = accountId,
            serviceEndpoints = endpoints.toList(),
        )
    }

    private fun endpoint(domain: String = "", ip: String, port: Int): HederaServiceEndpointResponse {
        return HederaServiceEndpointResponse(domainName = domain, ipAddressV4 = ip, port = port)
    }

    private companion object {
        const val BASE_URL = "https://mirror.example.com"
    }
}