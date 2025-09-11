package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.yieldsupply.addressfactory.YieldSupplyContractAddresses
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus
import java.math.BigDecimal

internal object DefaultYieldSupplyProvider : YieldSupplyProvider {

    override fun isSupported(): Boolean = false

    override fun getYieldSupplyContractAddresses(): YieldSupplyContractAddresses? = null

    override suspend fun getYieldContract(): String = EthereumUtils.ZERO_ADDRESS

    override suspend fun calculateYieldContract(): String = EthereumUtils.ZERO_ADDRESS

    override suspend fun getServiceFee(): BigDecimal = BigDecimal.ZERO

    override suspend fun getYieldSupplyStatus(tokenContractAddress: String): YieldSupplyStatus? = null

    override suspend fun getProtocolBalance(token: Token): BigDecimal = BigDecimal.ZERO

    override suspend fun isAllowedToSpend(tokenContractAddress: String): Boolean = false
}