package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus
import java.math.BigDecimal

internal object DefaultYieldSupplyProvider : YieldSupplyProvider {

    override fun factoryContractAddress(): String = ""

    override fun processorContractAddress(): String = ""

    override suspend fun getYieldContract(): String = ""

    override suspend fun calculateYieldContract(): String = ""

    override suspend fun getServiceFee(): BigDecimal = BigDecimal.ZERO

    override suspend fun getYieldSupplyStatus(tokenContractAddress: String): YieldSupplyStatus? = null

    override suspend fun getProtocolBalance(token: Token): BigDecimal = BigDecimal.ZERO

    override suspend fun isAllowedToSpend(tokenContractAddress: String): Boolean = false
}