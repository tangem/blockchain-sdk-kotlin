package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.blockchains.ethereum.tokenmethods.AllowanceERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ReadEthereumAddressEIP137CallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ReverseResolveENSAddressCallData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TokenBalanceERC20TokenCallData
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance

/**
 * Abstract base class for Ethereum-like JSON RPC providers
 * Supports different method prefixes (eth_, quai_, etc.)
 */
internal abstract class EthereumLikeJsonRpcProvider(
    override val baseUrl: String,
    private val postfixUrl: String = "",
    private val authToken: String? = null,
    private val nowNodesApiKey: String? = null,
) : NetworkProvider {

    private val api = createRetrofitInstance(baseUrl).create(EthereumApi::class.java)

    /**
     * Get the RPC method interface for this blockchain
     */
    protected abstract fun getMethods(): EthereumLikeMethod

    suspend fun getBalance(address: String): Result<JsonRPCResponse> {
        return createEthereumLikeBody(
            method = getMethods().getBalance,
            address,
            EthBlockParam.LATEST.value,
        ).post()
    }

    suspend fun getTokenBalance(data: EthereumTokenBalanceRequestData) = createEthereumLikeBody(
        method = getMethods().call,
        createTokenBalanceCallObject(address = data.address, contractAddress = data.contractAddress),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getTokenAllowance(data: EthereumTokenAllowanceRequestData) = createEthereumLikeBody(
        method = getMethods().call,
        createTokenAllowanceCallObject(data.ownerAddress, data.contractAddress, data.spenderAddress),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun resolveENSName(data: EthereumResolveENSNameRequestData) = createEthereumLikeBody(
        method = getMethods().call,
        createResolveENSDomainCallObject(data.contractAddress, data.nameBytes, data.callDataBytes),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun reverseResolveENSAddress(data: EthereumReverseResolveENSAddressRequestData) = createEthereumLikeBody(
        method = getMethods().call,
        createReverseResolveENSAddressCallObject(data.contractAddress, data.address),
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun call(data: Any) = createEthereumLikeBody(
        method = getMethods().call,
        data,
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getTxCount(address: String) = createEthereumLikeBody(
        method = getMethods().getTransactionCount,
        address,
        EthBlockParam.LATEST.value,
    ).post()

    suspend fun getPendingTxCount(address: String) = createEthereumLikeBody(
        method = getMethods().getTransactionCount,
        address,
        EthBlockParam.PENDING.value,
    ).post()

    suspend fun sendTransaction(transaction: String) = createEthereumLikeBody(
        method = getMethods().sendRawTransaction,
        transaction,
    ).post()

    suspend fun getGasLimit(call: EthCallObject) = createEthereumLikeBody(
        method = getMethods().estimateGas,
        call,
    ).post()

    suspend fun getGasPrice() = createEthereumLikeBody(method = getMethods().gasPrice).post()

    /**
     * Get fee history for 5 blocks (around a minute) with 25,50,75 percentiles (selected empirically)
     */
    @Suppress("MagicNumber")
    suspend fun getFeeHistory() = createEthereumLikeBody(
        method = getMethods().feeHistory,
        5, // blockCount
        EthBlockParam.LATEST.value, // newestBlock
        listOf(25, 50, 75), // rewardPercentiles
    ).post()

    private fun createEthereumLikeBody(method: String, vararg params: Any): JsonRPCRequest {
        return JsonRPCRequest(method = method, params = params, id = "67")
    }

    private fun createTokenBalanceCallObject(address: String, contractAddress: String) = EthCallObject(
        to = contractAddress,
        data = TokenBalanceERC20TokenCallData(address = address).dataHex,
    )

    private fun createTokenAllowanceCallObject(ownerAddress: String, contractAddress: String, spenderAddress: String) =
        EthCallObject(
            to = contractAddress,
            data = AllowanceERC20TokenCallData(
                ownerAddress = ownerAddress,
                spenderAddress = spenderAddress,
            ).dataHex,
        )

    private fun createResolveENSDomainCallObject(
        contractAddress: String,
        nameBytes: ByteArray,
        callDataBytes: ByteArray,
    ) = EthCallObject(
        to = contractAddress,
        data = ReadEthereumAddressEIP137CallData(
            nameBytes = nameBytes,
            callDataBytes = callDataBytes,
        ).dataHex,
    )

    private fun createReverseResolveENSAddressCallObject(contractAddress: String, address: ByteArray) = EthCallObject(
        to = contractAddress,
        data = ReverseResolveENSAddressCallData(address = address).dataHex,
    )

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
}