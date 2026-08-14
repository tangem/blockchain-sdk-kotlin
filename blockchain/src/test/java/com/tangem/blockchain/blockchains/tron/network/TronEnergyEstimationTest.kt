package com.tangem.blockchain.blockchains.tron.network

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * `triggerconstantcontract` answers HTTP 200 for a call that reverts, and still reports the energy
 * burned up to the revert. Passing that on would price a transaction that cannot succeed, so a
 * failed simulation has to surface as a failure rather than as a plausible fee.
 */
internal class TronEnergyEstimationTest {

    private val provider: TronNetworkProvider = mockk {
        every { baseUrl } returns "https://api.tron/"
    }

    private val service = TronNetworkService(
        rpcNetworkProviders = listOf(provider),
        blockchain = Blockchain.Tron,
    )

    @Test
    fun `GIVEN reverted simulation WHEN getMaxEnergyUseForCallData THEN failure`() = runTest {
        // Arrange
        // Shape of a real revert: `result.result` is still true and energy_used is non-zero, only
        // `message` marks the failure. The constant_result holds the error selector.
        stubSimulation(
            TronTriggerSmartContractResponse(
                constantResult = listOf("7939f424"),
                energyUsed = 45299,
                executionResult = TronContractExecutionResult(message = "REVERT opcode executed"),
            ),
        )

        // Act
        val actual = estimate()

        // Assert
        assertThat(actual).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `GIVEN successful simulation WHEN getMaxEnergyUseForCallData THEN energy returned`() = runTest {
        // Arrange
        stubSimulation(
            TronTriggerSmartContractResponse(
                constantResult = listOf(""),
                energyUsed = 160337,
                executionResult = TronContractExecutionResult(message = null),
            ),
        )

        // Act
        val actual = estimate()

        // Assert
        assertThat((actual as Result.Success).data).isEqualTo(160337L)
    }

    @Test
    fun `GIVEN response without result field WHEN getMaxEnergyUseForCallData THEN energy returned`() = runTest {
        // Arrange
        stubSimulation(TronTriggerSmartContractResponse(constantResult = listOf(""), energyUsed = 1000))

        // Act
        val actual = estimate()

        // Assert
        assertThat((actual as Result.Success).data).isEqualTo(1000L)
    }

    private fun stubSimulation(response: TronTriggerSmartContractResponse) {
        coEvery {
            provider.contractEnergyUsageForCallData(
                address = any(),
                contractAddress = any(),
                callDataHex = any(),
                callValue = any(),
            )
        } returns Result.Success(response)
    }

    private suspend fun estimate() = service.getMaxEnergyUseForCallData(
        address = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF",
        contractAddress = "TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn",
        callDataHex = "3110c7b9",
        callValue = 0,
    )
}