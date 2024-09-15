package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal

internal class EthereumJsonRpcProvider(
    override val baseUrl: String,
    private val postfixUrl: String = "",
    private val authToken: String? = null,
    private val nowNodesApiKey: String? = null,
) : NetworkProvider {

    private val api = createRetrofitInstance(baseUrl).create(EthereumApi::class.java)

    suspend fun getBalance(address: String) = createEthereumBody(
        method = EthereumMethod.GET_BALANCE,
        address,
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getTokenBalance(data: EthereumTokenBalanceRequestData) = createEthereumBody(
        method = EthereumMethod.CALL,
        createTokenBalanceCallObject(address = data.address, contractAddress = data.contractAddress),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getTokenAllowance(data: EthereumTokenAllowanceRequestData) = createEthereumBody(
        method = EthereumMethod.CALL,
        createTokenAllowanceCallObject(data.ownerAddress, data.contractAddress, data.spenderAddress),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun call(data: Any) = createEthereumBody(
        method = EthereumMethod.CALL,
        data,
        EthBlockParam.LATEST.value,
    ).post()

    @Suppress("LongParameterList")
    suspend fun callProcess(
        contractAddress: String,
        amount: BigDecimal,
        decimals: Int,
        cardAddress: String,
        otp: ByteArray,
        otpCounter: Int,
    ) = createEthereumBody(
        method = EthereumMethod.CALL,
        createProcessCallObject(contractAddress, amount, decimals, cardAddress, otp, otpCounter),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getTxCount(address: String) = createEthereumBody(
        method = EthereumMethod.GET_TRANSACTION_COUNT,
        address,
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getPendingTxCount(address: String) = createEthereumBody(
        method = EthereumMethod.GET_TRANSACTION_COUNT,
        address,
        EthBlockParam.PENDING.value,
    ).post()

    suspend fun sendTransaction(transaction: String) = createEthereumBody(
        method = EthereumMethod.SEND_RAW_TRANSACTION,
        transaction,
    ).post()

    suspend fun getGasLimit(call: EthCallObject) = createEthereumBody(
        method = EthereumMethod.ESTIMATE_GAS,
        call,
    ).post()

    suspend fun getGasPrice() = createEthereumBody(method = EthereumMethod.GAS_PRICE).post()

    /**
     * Get fee history for 5 blocks (around a minute) with 25,50,75 percentiles (selected empirically)
     *
     * @see <a href = https://www.quicknode.com/docs/ethereum/eth_feeHistory>API DOC<a/>
     */
    @Suppress("MagicNumber")
    suspend fun getFeeHistory() = createEthereumBody(
        method = EthereumMethod.FEE_HISTORY,
        5, // blockCount
        EthBlockParam.LATEST.value, // newestBlock
        listOf(25, 50, 75), // rewardPercentiles
    ).post()

    private fun createEthereumBody(method: EthereumMethod, vararg params: Any): JsonRPCRequest {
        return JsonRPCRequest(method = method.value, params = params, id = "67")
    }

    // TODO: [REDACTED_JIRA] Replace with SmartContractMethod interface implementations
    private fun createTokenBalanceCallObject(address: String, contractAddress: String) = EthCallObject(
        to = contractAddress,
        data = "0x70a08231000000000000000000000000" + address.removePrefixes(),
    )

    // TODO: [REDACTED_JIRA] Replace with SmartContractMethod interface implementations
    private fun createTokenAllowanceCallObject(ownerAddress: String, contractAddress: String, spenderAddress: String) =
        EthCallObject(
            to = contractAddress,
            // 5c9b5c6313a3746a1246d07bbedc0292da99f8e2000000000000000000000000e4c4693526e4e3a26f36311d3f80a193b2bae906
            data = buildString {
                append(tokenAllowanceSignature)
                append(CALL_DATA_SEPARATOR)
                append(ownerAddress.removePrefixes())
                append(CALL_DATA_SEPARATOR)
                append(spenderAddress.removePrefixes())
            },
        )

    @Suppress("LongParameterList")
    private fun createProcessCallObject(
        contractAddress: String,
        amount: BigDecimal,
        decimals: Int,
        cardAddress: String,
        otp: ByteArray,
        otpCounter: Int,
    ): EthCallObject {
        val data: String = EthereumUtils.createProcessData(
            cardAddress,
            amount.movePointLeft(decimals).toBigInteger(),
            otp,
            otpCounter,
        ).toHexString()
        return EthCallObject(to = contractAddress, data = data)
    }

    private suspend fun JsonRPCRequest.post(): Result<JsonRPCResponse> {
        return try {
            val result = retryIO {
                api.post(
                    body = this,
                    infuraProjectId = postfixUrl,
                    token = authToken,
                    nowNodesApiKey = nowNodesApiKey,
                )
            }
            Result.Success(result)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun String.removePrefixes(): String {
        return takeLast(ETH_VALUABLE_ADDRESS_PART_LENGTH)
    }

    companion object {
        private val tokenAllowanceSignature =
            "allowance(address,address)".toByteArray().toKeccak().copyOf(4).toHexString()

        private const val CALL_DATA_SEPARATOR = "000000000000000000000000"

        private const val ETH_VALUABLE_ADDRESS_PART_LENGTH = 40
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