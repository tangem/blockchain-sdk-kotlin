package com.tangem.blockchain.blockchains.aptos.network

import com.tangem.blockchain.blockchains.aptos.network.request.AptosTransactionBody
import com.tangem.blockchain.blockchains.aptos.network.response.AptosEstimateGasPriceResponse
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.blockchains.aptos.network.response.AptosSimulateTransactionBody
import com.tangem.blockchain.blockchains.aptos.network.response.AptosSubmitTransactionResponse
import retrofit2.http.*

/**
 * Aptos REST API
 *
 * @see <a href="https://fullnode.mainnet.aptoslabs.com/v1/spec#/">Aptos Node API</a>
 *
 * @author Andrew Khokhlov on 10/01/2024
 */
internal interface AptosApi {

    /**
     * Get account resources that contains information about account and balance
     *
     * @param address account address
     *
     * @see AptosResource to know more details about kind of resources
     */
    @Headers("Content-Type: application/json")
    @GET("v1/accounts/{address}/resources")
    suspend fun getAccountResources(@Path("address") address: String): List<AptosResource>

    /**
     * Gives an estimate of the gas unit price required to get a transaction on chain in a reasonable amount of time.
     * The gas unit price is the amount that each transaction commits to pay for each unit of gas consumed
     * in executing the transaction.
     */
    @Headers("Content-Type: application/json")
    @GET("v1/estimate_gas_price")
    suspend fun estimateGasPrice(): AptosEstimateGasPriceResponse

    /**
     * Simulate transaction's sending. Use it to estimate the maximum gas units for a submitted transaction.
     * Request queries:
     * - {estimate_gas_unit_price} - If set to true, the gas unit price in the transaction will be ignored and the
     *                                  estimated value will be used
     * - {estimate_max_gas_amount} - If set to true, the max gas value in the transaction will be ignored and the
     *                                  maximum possible gas will be used
     * - {estimate_prioritized_gas_unit_price} - If set to true, the transaction will use a higher price than the
     *                                              original estimate
     *
     * @param body raw transaction data without signing transaction hash
     */
    @Headers("Content-Type: application/json")
    @POST(
        "v1/transactions/simulate?" +
            "estimate_gas_unit_price=false&" +
            "estimate_max_gas_amount=true&" +
            "estimate_prioritized_gas_unit_price=false",
    )
    suspend fun simulateTransaction(@Body body: AptosTransactionBody): List<AptosSimulateTransactionBody>

    /** Submit transaction [body] */
    @Headers("Content-Type: application/json")
    @POST("v1/transactions")
    suspend fun submitTransaction(@Body body: AptosTransactionBody): AptosSubmitTransactionResponse
}
