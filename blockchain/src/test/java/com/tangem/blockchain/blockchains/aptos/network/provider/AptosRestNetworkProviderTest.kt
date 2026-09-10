package com.tangem.blockchain.blockchains.aptos.network.provider

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.network.AptosApi
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.ResultChecker
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal

internal class AptosRestNetworkProviderTest {

    private val api = mockk<AptosApi>()
    private val provider = AptosRestNetworkProvider(baseUrl = BASE_URL, api = api)

    @Test
    fun `balance is parsed from the view function response`() = runTest {
        coEvery { api.getAccountResources(ADDRESS) } returns listOf(AptosResource.AccountResource(SEQUENCE_NUMBER))
        coEvery { api.executeViewFunction(any()) } returns listOf(BALANCE_OCTAS)

        val actual = provider.getAccountInfo(ADDRESS)

        val expected = AptosAccountInfo(
            sequenceNumber = 7L,
            balance = BigDecimal(BALANCE_OCTAS),
            tokens = emptyList(),
        )
        assertThat((actual as Result.Success).data).isEqualTo(expected)
    }

    @Test
    fun `http error from the view function is a network failure, not a zero balance`() = runTest {
        coEvery { api.getAccountResources(ADDRESS) } returns listOf(AptosResource.AccountResource(SEQUENCE_NUMBER))
        coEvery { api.executeViewFunction(any()) } throws tooManyRequests()

        val actual = provider.getAccountInfo(ADDRESS)

        assertThat(actual).isInstanceOf(Result.Failure::class.java)
        assertThat(ResultChecker.isNetworkError(actual)).isTrue()
    }

    @Test
    fun `timeout on the view function is a network failure, not a zero balance`() = runTest {
        coEvery { api.getAccountResources(ADDRESS) } returns listOf(AptosResource.AccountResource(SEQUENCE_NUMBER))
        coEvery { api.executeViewFunction(any()) } throws IOException("timeout")

        val actual = provider.getAccountInfo(ADDRESS)

        assertThat(actual).isInstanceOf(Result.Failure::class.java)
        assertThat(ResultChecker.isNetworkError(actual)).isTrue()
    }

    @Test
    fun `empty view function response is a network failure, not a zero balance`() = runTest {
        coEvery { api.getAccountResources(ADDRESS) } returns listOf(AptosResource.AccountResource(SEQUENCE_NUMBER))
        coEvery { api.executeViewFunction(any()) } returns emptyList()

        val actual = provider.getAccountInfo(ADDRESS)

        assertThat((actual as Result.Failure).error).isInstanceOf(BlockchainSdkError.Aptos.Api::class.java)
        assertThat(ResultChecker.isNetworkError(actual)).isTrue()
    }

    @Test
    fun `account with no resources and zero balance is reported as not found`() = runTest {
        coEvery { api.getAccountResources(ADDRESS) } returns emptyList()
        coEvery { api.executeViewFunction(any()) } returns listOf("0")

        val actual = provider.getAccountInfo(ADDRESS)

        assertThat((actual as Result.Failure).error).isInstanceOf(BlockchainSdkError.AccountNotFound::class.java)
    }

    private fun tooManyRequests(): HttpException {
        val body = "rate limited".toResponseBody("text/plain".toMediaType())
        return HttpException(Response.error<Unit>(429, body))
    }

    private companion object {
        const val BASE_URL = "https://fullnode.mainnet.aptoslabs.com/"
        const val ADDRESS = "0x689c611c32ad1f5e16e446de2751f2ed1ec2fbdc66dfa45d4aa573f6dc431c8f"
        const val SEQUENCE_NUMBER = "7"
        const val BALANCE_OCTAS = "25702053156"
    }
}