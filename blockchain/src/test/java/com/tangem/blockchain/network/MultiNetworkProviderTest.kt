package com.tangem.blockchain.network

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

internal class MultiNetworkProviderTest {

    private val firstProvider = provider(url = "https://first.example.com/key-1")
    private val secondProvider = provider(url = "https://second.example.com/key-2")
    private val thirdProvider = provider(url = "https://third.example.com/key-3")

    private val multiProvider = MultiNetworkProvider(
        providers = listOf(firstProvider, secondProvider, thirdProvider),
        blockchain = Blockchain.Polygon,
    )

    @Test
    fun `performRequestOnEachHost queries every provider and keeps their order`() = runTest {
        coEvery { firstProvider.call(any()) } returns Result.Success(response("first"))
        coEvery { secondProvider.call(any()) } returns Result.Success(response("second"))
        coEvery { thirdProvider.call(any()) } returns Result.Success(response("third"))

        val results = multiProvider.performRequestOnEachHost(EthereumJsonRpcProvider::call, CALL_OBJECT)

        assertThat(results.map { (it.result as Result.Success).data.result })
            .containsExactly("first", "second", "third")
            .inOrder()
        assertThat(results.map { it.provider })
            .containsExactly(firstProvider, secondProvider, thirdProvider)
            .inOrder()
        coVerify(exactly = 1) { firstProvider.call(CALL_OBJECT) }
        coVerify(exactly = 1) { secondProvider.call(CALL_OBJECT) }
        coVerify(exactly = 1) { thirdProvider.call(CALL_OBJECT) }
    }

    @Test
    fun `performRequestOnEachHost asks the same host only once`() = runTest {
        val sameHostProvider = provider(url = "https://first.example.com/key-999")
        val multiProvider = MultiNetworkProvider(
            providers = listOf(firstProvider, sameHostProvider, secondProvider),
            blockchain = Blockchain.Polygon,
        )
        coEvery { firstProvider.call(any()) } returns Result.Success(response("first"))
        coEvery { secondProvider.call(any()) } returns Result.Success(response("second"))

        val results = multiProvider.performRequestOnEachHost(EthereumJsonRpcProvider::call, CALL_OBJECT)

        assertThat(results.map { it.provider }).containsExactly(firstProvider, secondProvider).inOrder()
        coVerify(exactly = 0) { sameHostProvider.call(any()) }
    }

    @Test
    fun `performRequestOnEachHost reports a failing provider without failing the whole call`() = runTest {
        coEvery { firstProvider.call(any()) } returns Result.Success(response("first"))
        coEvery { secondProvider.call(any()) } returns Result.Failure(BlockchainSdkError.CustomError("boom"))
        coEvery { thirdProvider.call(any()) } throws IOException("connection reset")

        val results = multiProvider.performRequestOnEachHost(EthereumJsonRpcProvider::call, CALL_OBJECT)

        assertThat(results).hasSize(3)
        assertThat(results[0].result).isInstanceOf(Result.Success::class.java)
        assertThat(results[1].result).isInstanceOf(Result.Failure::class.java)
        assertThat(results[2].result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `performRequestOnEachHost does not move the failover cursor`() = runTest {
        val providerBeforeCall = multiProvider.currentProvider
        coEvery { firstProvider.call(any()) } returns Result.Success(response("first"))
        coEvery { secondProvider.call(any()) } returns Result.Success(response("second"))
        coEvery { thirdProvider.call(any()) } returns Result.Success(response("third"))

        multiProvider.performRequestOnEachHost(EthereumJsonRpcProvider::call, CALL_OBJECT)

        assertThat(multiProvider.currentProvider).isSameInstanceAs(providerBeforeCall)
    }

    private fun provider(url: String) = mockk<EthereumJsonRpcProvider>(relaxed = true) {
        every { baseUrl } returns url
    }

    private fun response(result: String) = JsonRPCResponse(
        id = "1",
        jsonRpc = "2.0",
        result = result,
        error = null,
    )

    private companion object {
        val CALL_OBJECT = EthCallObject(
            to = "0x3333333333333333333333333333333333333333",
            data = "0x00",
        )
    }
}