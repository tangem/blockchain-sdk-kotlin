package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class EthereumJsonRpcProvider(
    baseUrl: String,
    private val postfixUrl: String = "",
    private val authToken: String? = null,
    private val nowNodesApiKey: String? = null,
    private val getBlockApiKey: String? = null,
) {

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

    suspend fun getTokenAllowance(data: EthereumTokenAllowanceRequestData) =
        createEthereumBody(
            EthereumMethod.CALL,
            createTokenAllowanceCallObject(data.ownerAddress, data.contractAddress, data.spenderAddress),
            EthBlockParam.LATEST.value
        ).post()

    suspend fun call(data: Any): Result<EthereumResponse> {
        return createEthereumBody(EthereumMethod.CALL, data, EthBlockParam.LATEST.value).post()
    }

    suspend fun callProcess(
        contractAddress: String,
        amount: BigDecimal,
        decimals: Int,
        cardAddress: String,
        otp: ByteArray,
        otpCounter: Int
    ) = createEthereumBody(
        EthereumMethod.CALL,
        createProcessCallObject(contractAddress, amount, decimals, cardAddress, otp, otpCounter),
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
        contractAddress: String,
    ) = EthCallObject(
        to = contractAddress,
        data = "0x70a08231000000000000000000000000" + address.substring(2)
    )

    private fun createTokenAllowanceCallObject(
        ownerAddress: String,
        contractAddress: String,
        spenderAddress: String,
    ) = EthCallObject(
        to = contractAddress,
        //5c9b5c6313a3746a1246d07bbedc0292da99f8e2000000000000000000000000e4c4693526e4e3a26f36311d3f80a193b2bae906
        data = "0xdd62ed3e000000000000000000000000" + ownerAddress.substring(2) + "000000000000000000000000" + spenderAddress.substring(2)
    )

    private fun createProcessCallObject(
        contractAddress: String,
        amount: BigDecimal,
        decimals: Int,
        cardAddress: String,
        otp: ByteArray,
        otpCounter: Int,
    ): EthCallObject {
        val data: String = "0x" + EthereumUtils.createProcessData(
            cardAddress,
            amount.movePointLeft(decimals).toBigInteger(),
            otp,
            otpCounter
        ).toHexString()
        return EthCallObject(to = contractAddress, data = data)
    }

    private suspend fun EthereumBody.post(): Result<EthereumResponse> {
        return try {
            val result = retryIO {
                api.post(
                    body = this,
                    infuraProjectId = postfixUrl,
                    token = authToken,
                    nowNodesApiKey = nowNodesApiKey,
                    getBlockApiKey = getBlockApiKey
                )
            }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }
}

data class EthereumTokenBalanceRequestData(
    val address: String,
    val contractAddress: String,
)

data class EthereumTokenAllowanceRequestData(
    val ownerAddress: String,
    val contractAddress: String,
    val spenderAddress: String,
)

data class EthereumGasLimitRequestData(
    val to: String,
    val from: String,
    val data: String?,
)