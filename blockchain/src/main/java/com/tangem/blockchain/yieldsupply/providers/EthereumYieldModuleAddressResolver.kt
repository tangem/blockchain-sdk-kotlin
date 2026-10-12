package com.tangem.blockchain.yieldsupply.providers

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.network.EthCallObject
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.getUrlWithoutSensitiveInfo
import com.tangem.blockchain.network.moshi

@OptIn(ExperimentalStdlibApi::class)
internal class EthereumYieldModuleAddressResolver(
    private val multiJsonRpcProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
) {

    private val stringAdapter by lazy { moshi.adapter<String>() }
    private val addressService = EthereumAddressService()

    suspend fun resolve(factoryContractAddress: String, callDataHex: String): String {
        val responses = multiJsonRpcProvider.performRequestOnEachHost(
            request = EthereumJsonRpcProvider::call,
            data = EthCallObject(
                to = factoryContractAddress,
                data = callDataHex,
            ),
        )

        val answers = responses.mapNotNull { (provider, result) ->
            result.extractAddressOrNull()?.let { provider to it }
        }

        val addresses = answers.map { (_, address) -> address }.distinctBy(String::lowercase)

        return when {
            addresses.isEmpty() -> EthereumUtils.ZERO_ADDRESS
            addresses.size > 1 -> {
                logMismatch(answers)
                throw BlockchainSdkError.Ethereum.YieldModuleAddressMismatch(addresses)
            }
            answers.size >= MIN_CONFIRMATIONS -> addresses.first()
            else -> {
                logNotCorroborated()
                EthereumUtils.ZERO_ADDRESS
            }
        }
    }

    private fun logMismatch(answers: List<Pair<EthereumJsonRpcProvider, String>>) {
        val details = try {
            answers.joinToString { (provider, address) ->
                "${provider.baseUrl.getUrlWithoutSensitiveInfo()}=$address"
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            answers.joinToString { (_, address) -> address }
        }

        Logger.e(TAG, "Yield module address mismatch between providers: $details")
    }

    private fun logNotCorroborated() {
        Logger.e(TAG, "Yield module address not corroborated: only one provider answered")
    }

    private fun Result<JsonRPCResponse>.extractAddressOrNull(): String? {
        val rawAddress = try {
            extractYieldRpcResult(stringAdapter)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "Failed to read yield module address from provider: ${e.message}")
            return null
        }

        val address = HEX_PREFIX + rawAddress.takeLast(EthereumUtils.ADDRESS_HEX_LENGTH)

        return address.takeIf(addressService::validate)
    }

    private companion object {
        const val TAG = "EthereumYieldModuleAddressResolver"
        const val HEX_PREFIX = "0x"
        const val MIN_CONFIRMATIONS = 2
    }
}