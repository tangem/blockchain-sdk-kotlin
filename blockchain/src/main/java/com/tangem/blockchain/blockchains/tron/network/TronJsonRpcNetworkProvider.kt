package com.tangem.blockchain.blockchains.tron.network

import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString

class TronJsonRpcNetworkProvider(override val network: TronNetwork) : TronNetworkProvider {

    private val api: TronApi by lazy {
        createRetrofitInstance(network.url).create(TronApi::class.java)
    }

    override suspend fun getAccount(address: String): Result<TronGetAccountResponse> {
        return try {
            val response = retryIO { api.getAccount(TronGetAccountRequest(address, true)) }
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getAccountResource(address: String): Result<TronGetAccountResourceResponse> {
        return try {
            val response =
                retryIO { api.getAccountResource(TronGetAccountRequest(address, true)) }
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getNowBlock(): Result<TronBlock> {
        return try {
            val response = retryIO { api.getNowBlock() }
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun broadcastHex(data: ByteArray): Result<TronBroadcastResponse> {
        return try {
            val response =
                retryIO { api.broadcastHex(TronBroadcastRequest(data.toHexString())) }
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getTokenBalance(
        address: String,
        contractAddress: String
    ): Result<TronTriggerSmartContractResponse> {
        return try {
            val response = retryIO {
                api.getTokenBalance(
                    requestBody = TronTriggerSmartContractRequest(
                        ownerAddress = address,
                        contractAddress = contractAddress,
                        functionSelector = "balanceOf(address)",
                        parameter = TronAddressService.toHexForm(address, 64) ?: "",
                        visible = true
                    ),
                )
            }
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getTokenTransactionHistory(contractAddress: String): Result<TronTokenHistoryResponse> {
        return try {
            val response = retryIO { api.getTokenTransactionHistory(contractAddress) }
            Result.Success(response)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getTransactionInfoById(id: String): Result<String> {
        return try {
            val response =
                retryIO { api.getTransactionInfoById(TronTransactionInfoRequest(id)) }
            Result.Success(response.id)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }
}