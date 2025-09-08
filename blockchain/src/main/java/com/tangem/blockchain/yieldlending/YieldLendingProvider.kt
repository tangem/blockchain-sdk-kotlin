package com.tangem.blockchain.yieldlending

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.yieldlending.providers.ethereum.EthereumLendingStatus
import java.math.BigDecimal

interface YieldLendingProvider {

    val factoryContractAddress: String

    val processorContractAddress: String

    suspend fun getServiceFee(): BigDecimal

    suspend fun getYieldContract(): String

    suspend fun getLendingStatus(tokenContractAddress: String): EthereumLendingStatus?

    suspend fun getLentBalance(token: Token): Amount

    suspend fun getProtocolBalance(token: Token): BigDecimal

    suspend fun isLent(token: Token): Boolean

    suspend fun isAllowedToSpend(tokenContractAddress: String): Boolean

}