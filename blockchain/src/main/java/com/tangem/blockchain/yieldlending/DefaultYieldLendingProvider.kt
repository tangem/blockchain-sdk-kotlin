package com.tangem.blockchain.yieldlending

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.yieldlending.providers.ethereum.EthereumLendingStatus
import java.math.BigDecimal

internal object DefaultYieldLendingProvider : YieldLendingProvider {
    override val factoryContractAddress: String = ""
    override val processorContractAddress: String = ""

    override suspend fun getYieldContract(): String = ""

    override suspend fun getServiceFee(): BigDecimal = BigDecimal.ZERO

    override suspend fun getLendingStatus(tokenContractAddress: String): EthereumLendingStatus? = null

    override suspend fun getLentBalance(token: Token): Amount = Amount(Blockchain.Unknown)

    override suspend fun getProtocolBalance(token: Token): BigDecimal = BigDecimal.ZERO

    override suspend fun isLent(token: Token): Boolean = false

    override suspend fun isAllowedToSpend(tokenContractAddress: String): Boolean = false
}