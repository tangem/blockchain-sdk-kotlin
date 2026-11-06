package com.tangem.blockchain.yieldsupply

import com.google.common.truth.Truth
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.common.extensions.hexToBytes
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class YieldModuleUpgradeTest {

    private val callData = CompiledSmartContractCallData("0xdeadbeef".hexToBytes())
    private val provider = mockk<YieldSupplyProvider>()

    @Test
    fun `WHEN upgrade available THEN call is wrapped into latest implementation`() = runTest {
        coEvery { provider.checkModuleVersionStatus() } returns YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = CURRENT_IMPLEMENTATION,
            latestImplementation = LATEST_IMPLEMENTATION,
        )

        val actual = provider.wrapWithYieldModuleUpgradeIfAvailable(callData)

        Truth.assertThat(actual).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
        val wrapped = actual as EthereumYieldSupplyUpgradeToAndCallCallData
        Truth.assertThat(wrapped.newImplementation).isEqualTo(LATEST_IMPLEMENTATION)
        Truth.assertThat(wrapped.innerCallData).isSameInstanceAs(callData)
    }

    @Test
    fun `WHEN no upgrade is available THEN call is returned as is`() = runTest {
        val statuses = listOf(
            YieldModuleVersionStatus.UpToDate,
            YieldModuleVersionStatus.NotDeployed,
            YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = CURRENT_IMPLEMENTATION),
            YieldModuleVersionStatus.Indeterminate(reason = "rpc"),
        )

        statuses.forEach { status ->
            coEvery { provider.checkModuleVersionStatus() } returns status

            Truth.assertWithMessage(status.toString())
                .that(provider.wrapWithYieldModuleUpgradeIfAvailable(callData))
                .isSameInstanceAs(callData)
        }
    }

    @Test
    fun `WHEN version check throws THEN call is returned as is`() = runTest {
        coEvery { provider.checkModuleVersionStatus() } throws IllegalStateException("rpc failed")

        val actual = provider.wrapWithYieldModuleUpgradeIfAvailable(callData)

        Truth.assertThat(actual).isSameInstanceAs(callData)
    }

    private companion object {
        const val CURRENT_IMPLEMENTATION = "0x4444444444444444444444444444444444444444"
        const val LATEST_IMPLEMENTATION = "0x5555555555555555555555555555555555555555"
    }
}