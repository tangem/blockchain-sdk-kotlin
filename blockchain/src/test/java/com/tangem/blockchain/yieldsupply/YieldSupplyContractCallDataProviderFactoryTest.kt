package com.tangem.blockchain.yieldsupply

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyWithdrawCallData
import com.tangem.common.extensions.toHexString
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

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

        assertThat(result).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
        val wrapped = result as EthereumYieldSupplyUpgradeToAndCallCallData
        assertThat(wrapped.newImplementation).isEqualTo(latestImpl)
        assertThat(wrapped.innerCallData).isSameInstanceAs(originalCallData)
        assertThat(wrapped.data.toHexString().lowercase()).startsWith(UPGRADE_TO_AND_CALL_SELECTOR)
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

    @Test
    fun `wrapWithUpgradeIfAvailable wraps call data when module has UpgradeAvailable`() {
        val latestImpl = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfAvailable(
            versionStatus = YieldModuleVersionStatus.UpgradeAvailable(
                currentImplementation = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                latestImplementation = latestImpl,
            ),
            originalCallData = originalCallData,
        )

        assertThat(result).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
        val wrapped = result as EthereumYieldSupplyUpgradeToAndCallCallData
        assertThat(wrapped.newImplementation).isEqualTo(latestImpl)
        assertThat(wrapped.innerCallData).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `wrapWithUpgradeIfAvailable returns original call data for UpToDate`() {
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfAvailable(
            versionStatus = YieldModuleVersionStatus.UpToDate,
            originalCallData = originalCallData,
        )

        assertThat(result).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `wrapWithUpgradeIfAvailable returns original call data for NotDeployed`() {
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfAvailable(
            versionStatus = YieldModuleVersionStatus.NotDeployed,
            originalCallData = originalCallData,
        )

        assertThat(result).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `wrapWithUpgradeIfAvailable returns original call data for UpgradeUnavailable`() {
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfAvailable(
            versionStatus = YieldModuleVersionStatus.UpgradeUnavailable(
                currentImplementation = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            ),
            originalCallData = originalCallData,
        )

        assertThat(result).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `wrapWithUpgradeIfAvailable returns original call data for Indeterminate`() {
        val result = YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfAvailable(
            versionStatus = YieldModuleVersionStatus.Indeterminate(reason = "rpc down"),
            originalCallData = originalCallData,
        )

        assertThat(result).isSameInstanceAs(originalCallData)
    }

    @Test
    fun `getWithdrawCallData returns withdraw call data`() {
        val amount = Amount(
            token = Token(
                symbol = "USDC",
                contractAddress = "0x1234567890abcdef1234567890abcdef12345678",
                decimals = 6,
            ),
            value = BigDecimal("0.01"),
        )
        val callData = YieldSupplyContractCallDataProviderFactory.getWithdrawCallData(
            tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678",
            amount = amount,
        )
        assertThat(callData).isInstanceOf(EthereumYieldSupplyWithdrawCallData::class.java)
        assertThat(callData.methodId).isEqualTo("0xf3fef3a3")
    }

    private companion object {
        // keccak256("upgradeToAndCall(address,bytes)")[:4]
        const val UPGRADE_TO_AND_CALL_SELECTOR = "4f1ef286"
    }
}