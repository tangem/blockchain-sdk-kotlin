package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.AddHeaderInterceptor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString

class TronJsonRpcNetworkProvider(override val network: TronNetwork) : TronNetworkProvider {

    override val baseUrl: String = network.url

    private val api: TronApi by lazy {
        val headerInterceptors = when (network) {
            is TronNetwork.TronGrid -> {
                if (!network.apiKey.isNullOrEmpty()) {
                    listOf(AddHeaderInterceptor(mapOf(TRON_GRID_API_HEADER_NAME to network.apiKey)))
                } else {
                    emptyList()
                }
            }
            is TronNetwork.NowNodes -> {
                if (network.apiKey.isNotEmpty()) {
                    listOf(AddHeaderInterceptor(mapOf(NowNodeCredentials.headerApiKey to network.apiKey)))
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }

        createRetrofitInstance(network.url, headerInterceptors).create(TronApi::class.java)
    }

    override suspend fun getAccount(address: String): Result<TronGetAccountResponse> {
        return try {
            val response = api.getAccount(requestBody = TronGetAccountRequest(address, true))
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getAccountResource(address: String): Result<TronGetAccountResourceResponse> {
        return try {
            val response = api.getAccountResource(requestBody = TronGetAccountRequest(address, true))
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getNowBlock(): Result<TronBlock> {
        return try {
            val response = api.getNowBlock()
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun broadcastHex(data: ByteArray): Result<TronBroadcastResponse> {
        return try {
            val response = api.broadcastHex(TronBroadcastRequest(data.toHexString()))
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    @Suppress("MagicNumber")
    override suspend fun getTokenBalance(
        tokenBalanceRequestData: TokenBalanceRequestData,
    ): Result<TronTriggerSmartContractResponse> {
        return try {
            val response = api.getTokenBalance(
                requestBody = TronTriggerSmartContractRequest(
                    ownerAddress = tokenBalanceRequestData.address,
                    contractAddress = tokenBalanceRequestData.contractAddress,
                    functionSelector = "balanceOf(address)",
                    parameter = TronAddressService.toHexForm(tokenBalanceRequestData.address, 64) ?: "",
                    visible = true,
                ),
            )
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun contractEnergyUsage(
        address: String,
        contractAddress: String,
        parameter: String,
    ): Result<TronTriggerSmartContractResponse> {
        return try {
            val response = api.getTokenBalance(
                requestBody = TronTriggerSmartContractRequest(
                    ownerAddress = address,
                    contractAddress = contractAddress,
                    functionSelector = "transfer(address,uint256)",
                    parameter = parameter,
                    visible = true,
                ),
            )
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getTransactionInfoById(id: String): Result<String> {
        return try {
            val response = api.getTransactionInfoById(TronTransactionInfoRequest(id))
            Result.Success(response.id)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getChainParameters(): Result<TronChainParametersResponse> {
        return try {
            val response = api.getChainParameters()
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    companion object {
        const val TRON_GRID_API_HEADER_NAME = "TRON-PRO-API-KEY"
    }
}
