package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import kotlinx.coroutines.CancellationException

private const val TAG = "YieldModuleUpgrade"

/** A failed version check is not fatal: the original call data is returned, as if no upgrade existed. */
@Suppress("TooGenericExceptionCaught")
suspend fun YieldSupplyProvider.wrapWithYieldModuleUpgradeIfAvailable(
    callData: SmartContractCallData,
): SmartContractCallData {
    val versionStatus = try {
        checkModuleVersionStatus()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e(TAG, "Yield module version check failed, sending call without upgrade", e)
        return callData
    }

    return YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfAvailable(versionStatus, callData)
}