package com.tangem.blockchain.yieldsupply

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.common.extensions.toHexString
import org.junit.Assert.assertThrows
import org.junit.Test

internal class YieldSupplyContractCallDataProviderFactoryTest {

    private val originalCallData = EthereumYieldSupplyEnterCallData(
        tokenContractAddress = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    )

    @Test
    fun `wrapWithUpgradeIfNeeded returns original call data when module is UpToDate`() {
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfNeeded(
            versionStatus = YieldModuleVersionStatus.UpToDate,
            originalCallData = originalCallData,
        )

        assertThat(result).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `wrapWithUpgradeIfNeeded returns original call data when module is NotDeployed`() {
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfNeeded(
            versionStatus = YieldModuleVersionStatus.NotDeployed,
            originalCallData = originalCallData,
        )

        assertThat(result).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `wrapWithUpgradeIfNeeded wraps call data when module has UpgradeAvailable`() {
        val latestImpl = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfNeeded(
            versionStatus = YieldModuleVersionStatus.UpgradeAvailable(
                currentImplementation = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                latestImplementation = latestImpl,
            ),
            originalCallData = originalCallData,
        )

        // Must be an upgradeToAndCall wrapper. Verify via the public byte output (private fields
        // are not accessible): selector + encoded latest impl address + embedded original data.
        // Anything else would route the swap to the wrong proxy or skip the upgrade entirely.
        assertThat(result).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
        val wrappedHex = result.data.toHexString().lowercase()
        assertThat(wrappedHex).startsWith(UPGRADE_TO_AND_CALL_SELECTOR)
        assertThat(wrappedHex).contains(latestImpl.removePrefix("0x").lowercase())
        assertThat(wrappedHex).contains(originalCallData.data.toHexString().lowercase())
    }

    @Test
    fun `wrapWithUpgradeIfNeeded throws YieldModuleUpgradeUnavailableException when upgrade is unavailable`() {
        val exception = assertThrows(YieldModuleUpgradeUnavailableException::class.java) {
            YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfNeeded(
                versionStatus = YieldModuleVersionStatus.UpgradeUnavailable(
                    currentImplementation = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                ),
                originalCallData = originalCallData,
            )
        }

        assertThat(exception.currentImplementation)
            .isEqualTo("0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
    }

    @Test
    fun `wrapWithUpgradeIfNeeded throws YieldModuleVersionIndeterminateException when version is indeterminate`() {
        val exception = assertThrows(YieldModuleVersionIndeterminateException::class.java) {
            YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfNeeded(
                versionStatus = YieldModuleVersionStatus.Indeterminate(reason = "rpc down"),
                originalCallData = originalCallData,
            )
        }

        assertThat(exception.reason).isEqualTo("rpc down")
    }

    private companion object {
        // keccak256("upgradeToAndCall(address,bytes)")[:4]
        const val UPGRADE_TO_AND_CALL_SELECTOR = "4f1ef286"
    }
}