package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus
import java.math.BigDecimal

/**
 *  Interface defining a provider for yield module services.
 */
interface YieldSupplyProvider {

    /** Address of the factory contract used to create yield contracts */
    fun factoryContractAddress(): String

    /** Address of the processor contract that handles yield processing */
    fun processorContractAddress(): String

    /**
     *  Retrieves the service fee associated with yield module.
     *
     *  @return The service fee as a [BigDecimal].
     */
    suspend fun getServiceFee(): BigDecimal

    /**
     *  Retrieves the address of the deployed yield contract.
     *
     *  @return The yield contract address as a [String].
     */
    suspend fun getYieldContract(): String

    /**
     *  Calculates and retrieves the yield contract address for the wallet address
     *
     *  @return The calculated yield contract address as a [String].
     */
    suspend fun calculateYieldContract(): String

    /**
     *  Retrieves the status of a specific yield token.
     *
     *  @param tokenContractAddress The contract address of the yield token.
     *  @return The [YieldSupplyStatus] containing status information, or null if not found.
     */
    suspend fun getYieldSupplyStatus(tokenContractAddress: String): YieldSupplyStatus?

    /**
     *  Retrieves the total balance of the underlying protocol assets for a specific yield token.
     *
     *  @param token The yield token for which to retrieve the protocol balance.
     *  @return The protocol balance as a [BigDecimal].
     */
    suspend fun getProtocolBalance(token: Token): BigDecimal

    /**
     *  Checks if the wallet is allowed to spend the specified yield token.
     *
     *  @param tokenContractAddress The contract address of the yield token.
     *  @return True if allowed to spend, false otherwise.
     */
    suspend fun isAllowedToSpend(tokenContractAddress: String): Boolean
}