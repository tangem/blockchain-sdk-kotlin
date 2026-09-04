package com.tangem.blockchain.blockchains.ethereum.network

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

internal class EthereumLikeNetworkServiceTest {

    private val multiJsonRpcProvider = mockk<MultiNetworkProvider<EthereumJsonRpcProvider>>(relaxed = true)
    private val yieldSupplyProvider = mockk<YieldSupplyProvider>(relaxed = true)

    private val service = EthereumNetworkService(
        multiJsonRpcProvider = multiJsonRpcProvider,
        yieldSupplyProvider = yieldSupplyProvider,
    )

    @Test
    fun `getTokensBalance falls back to the plain balance when providers disagree on the yield module`() = runTest {
        coEvery { yieldSupplyProvider.isSupported() } returns true
        coEvery { yieldSupplyProvider.getYieldModuleAddress() } throws
            BlockchainSdkError.Ethereum.YieldModuleAddressMismatch(listOf(MODULE, HOSTILE_MODULE))
        stubPlainTokenBalance()

        val result = service.getTokensBalance(ADDRESS, setOf(FIRST_TOKEN, SECOND_TOKEN))

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.map { it.value })
            .containsExactly(ONE_TOKEN, ONE_TOKEN)
    }

    @Test
    fun `getTokensBalance keeps using the yield balance when providers agree`() = runTest {
        coEvery { yieldSupplyProvider.isSupported() } returns true
        coEvery { yieldSupplyProvider.getYieldModuleAddress() } returns MODULE
        coEvery { yieldSupplyProvider.getYieldSupplyStatus(any()) } returns YieldSupplyStatus(
            isInitialized = true,
            isActive = true,
            maxNetworkFee = BigDecimal.ZERO,
        )
        coEvery { yieldSupplyProvider.getBalance(any(), any()) } returns Amount(
            token = FIRST_TOKEN,
            value = YIELD_BALANCE,
        )

        val result = service.getTokensBalance(ADDRESS, setOf(FIRST_TOKEN))

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.map { it.value }).containsExactly(YIELD_BALANCE)
    }

    @Test
    fun `getTokensBalance still fails when the plain balance request fails`() = runTest {
        coEvery { yieldSupplyProvider.isSupported() } returns false
        coEvery {
            multiJsonRpcProvider.performRequest(
                EthereumLikeJsonRpcProvider::getTokenBalance,
                any<EthereumTokenBalanceRequestData>(),
            )
        } returns Result.Failure(BlockchainSdkError.CustomError("rpc down"))

        val result = service.getTokensBalance(ADDRESS, setOf(FIRST_TOKEN))

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    private fun stubPlainTokenBalance() {
        coEvery {
            multiJsonRpcProvider.performRequest(
                EthereumLikeJsonRpcProvider::getTokenBalance,
                any<EthereumTokenBalanceRequestData>(),
            )
        } returns Result.Success(
            JsonRPCResponse(id = "1", jsonRpc = "2.0", result = ONE_TOKEN_RAW, error = null),
        )
    }

    private companion object {
        const val ADDRESS = "0x1111111111111111111111111111111111111111"
        const val MODULE = "0x7777777777777777777777777777777777777777"
        const val HOSTILE_MODULE = "0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
        const val ONE_TOKEN_RAW = "0x0de0b6b3a7640000"

        val ONE_TOKEN: BigDecimal = BigDecimal.ONE.setScale(18)
        val YIELD_BALANCE: BigDecimal = BigDecimal("42")

        val FIRST_TOKEN = Token(
            name = "First",
            symbol = "FST",
            contractAddress = "0x2222222222222222222222222222222222222222",
            decimals = 18,
        )

        val SECOND_TOKEN = Token(
            name = "Second",
            symbol = "SND",
            contractAddress = "0x3333333333333333333333333333333333333333",
            decimals = 18,
        )
    }
}