package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.network.createRetrofitInstance

class EthereumJsonRpcProvider(baseUrl: String, private val apiKey: String) {

    private val api = createRetrofitInstance(baseUrl).create(EthereumApi::class.java)

    suspend fun getBalance(address: String) =
            createEthereumBody(
                    EthereumMethod.GET_BALANCE,
                    address,
                    EthBlockParam.LATEST.value
            ).post()

    suspend fun getTokenBalance(address: String, contractAddress: String) =
            createEthereumBody(
                    EthereumMethod.CALL,
                    createTokenBalanceCallObject(address, contractAddress),
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

    suspend fun getGasLimit(to: String, from: String, data: String?) =
            createEthereumBody(EthereumMethod.ESTIMATE_GAS, EthCallObject(to, from, data)).post()

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

    private suspend fun EthereumBody.post() = api.post(this, apiKey)
}