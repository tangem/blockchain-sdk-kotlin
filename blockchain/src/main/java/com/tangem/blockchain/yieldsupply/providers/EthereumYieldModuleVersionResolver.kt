package com.tangem.blockchain.yieldsupply.providers

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.moshi
import com.tangem.blockchain.yieldsupply.addressfactory.YieldSupplyContractAddresses
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyImplementationCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyModuleFactoryCallData

private const val TAG = "EthereumYieldModuleVersionResolver"

/**
 * Decode a JSON-RPC string result, mapping protocol/transport failures to a [BlockchainSdkError].
 */
internal fun <Body> Result<JsonRPCResponse>.extractYieldRpcResult(adapter: JsonAdapter<Body>): Body {
    return when (this) {
        is Result.Success ->
            runCatching { adapter.fromJsonValue(data.result) }.getOrNull()
                ?: throw data.error?.let { error ->
                    BlockchainSdkError.Ethereum.getApiErrorByCode(code = error.code, message = error.message)
                } ?: BlockchainSdkError.CustomError("Unknown response format")
        is Result.Failure ->
            throw error as? BlockchainSdkError ?: BlockchainSdkError.CustomError("Unknown error format")
    }
}

/**
 * Resolve the upgrade status of an outdated yield module.
 */
@OptIn(ExperimentalStdlibApi::class)
internal suspend fun resolveOutdatedYieldModuleStatus(
    multiJsonRpcProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    contractAddresses: YieldSupplyContractAddresses,
    yieldModuleAddress: String,
    currentImpl: String,
    latestKnown: String,
): YieldModuleVersionStatus = try {
    val stringAdapter = moshi.adapter<String>()

    val moduleFactoryAddress = HEX_PREFIX + multiJsonRpcProvider.performRequest(
        request = EthereumJsonRpcProvider::call,
        data = EthCallObject(
            to = yieldModuleAddress,
            data = EthereumYieldSupplyModuleFactoryCallData.dataHex,
        ),
    ).extractYieldRpcResult(stringAdapter).takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

    if (!moduleFactoryAddress.equals(contractAddresses.factoryContractAddress, ignoreCase = true)) {
        Log.w(
            TAG,
            "Module factory $moduleFactoryAddress differs from configured " +
                "${contractAddresses.factoryContractAddress}; upgrade to $latestKnown is not " +
                "authorized by this module's factory",
        )
    }

    val factoryImpl = multiJsonRpcProvider.performRequest(
        request = EthereumJsonRpcProvider::call,
        data = EthCallObject(
            to = moduleFactoryAddress,
            data = EthereumYieldSupplyImplementationCallData.dataHex,
        ),
    ).extractYieldRpcResult(stringAdapter)

    val factoryImplAddress = HEX_PREFIX + factoryImpl.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

    if (factoryImplAddress.equals(latestKnown, ignoreCase = true)) {
        YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = currentImpl,
            latestImplementation = latestKnown,
        )
    } else {
        YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = currentImpl)
    }
} catch (e: Exception) {
    Log.w(TAG, "Failed to check factory implementation: ${e.message}")
    YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = currentImpl)
}