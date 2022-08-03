package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.toBlockchainCustomError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance

class EthereumJsonRpcProvider(baseUrl: String, private val postfixUrl: String = "") {

    val host: String = baseUrl

    private val api = createRetrofitInstance(baseUrl).create(EthereumApi::class.java)

    suspend fun getBalance(address: String) =
        createEthereumBody(
            EthereumMethod.GET_BALANCE,
            address,
            EthBlockParam.LATEST.value
        ).post()

    suspend fun getTokenBalance(data: EthereumTokenBalanceRequestData) =
        createEthereumBody(
            EthereumMethod.CALL,
            createTokenBalanceCallObject(data.address, data.contractAddress),
            EthBlockParam.LATEST.value
        ).post()

    suspend fun getTxCount(address: String) =
        createEthereumBody(
            EthereumMethod.GET_TRANSACTION_COUNT,
            address,
            EthBlockParam.LATEST.value
        ).post()

    suspend fun getPendingTxCount(address: String) =
        createEthereumBody(
            EthereumMethod.GET_TRANSACTION_COUNT,
            address,
            EthBlockParam.PENDING.value
        ).post()

    suspend fun sendTransaction(transaction: String) =
        createEthereumBody(EthereumMethod.SEND_RAW_TRANSACTION, transaction).post()

    suspend fun getGasLimit(call: EthCallObject) =
        createEthereumBody(EthereumMethod.ESTIMATE_GAS, call).post()

    suspend fun getGasPrice() = createEthereumBody(EthereumMethod.GAS_PRICE).post()


    private fun createEthereumBody(method: EthereumMethod, vararg params: Any) =
        EthereumBody(method.value, params.toList())

    private fun createTokenBalanceCallObject(
        address: String,
        contractAddress: String
    ) = EthCallObject(
        to = contractAddress,
        data = "0x70a08231000000000000000000000000" + address.substring(2),
    )

    private suspend fun EthereumBody.post(): Result<EthereumResponse> {
        return try {
            val result = retryIO { api.post(this, postfixUrl) }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainCustomError())
        }
    }

    companion object {
        fun infura(baseUrl: String, infuraProjectId: String) =
            EthereumJsonRpcProvider(baseUrl + "v3/", infuraProjectId)

        fun classic(baseUrl: String, postfixUrl: String) =
            EthereumJsonRpcProvider(baseUrl, postfixUrl)
    }
}

data class EthereumTokenBalanceRequestData(
    val address: String,
    val contractAddress: String
)

data class EthereumGasLimitRequestData(
    val to: String,
    val from: String,
    val data: String?
)