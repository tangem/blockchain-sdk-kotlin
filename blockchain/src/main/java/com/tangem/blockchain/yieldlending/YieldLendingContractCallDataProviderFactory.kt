package com.tangem.blockchain.yieldlending

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.factory.EthereumYieldLendingDeployCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingEnterCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingExitCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingInitTokenCallData
import com.tangem.blockchain.yieldlending.providers.ethereum.yield.EthereumYieldLendingReactivateTokenCallData
import java.math.BigInteger

object YieldLendingContractCallDataProviderFactory {

    fun getDeployCallData(
        walletAddress: String,
        tokenContractAddress: String,
        maxNetworkFee: Amount,
    ): SmartContractCallData = EthereumYieldLendingDeployCallData(
        address = walletAddress,
        tokenContractAddress = tokenContractAddress,
        maxNetworkFee = maxNetworkFee,
    )

    fun getInitTokenCallData(
        tokenContractAddress: String,
        maxNetworkFee: Amount,
    ): SmartContractCallData = EthereumYieldLendingInitTokenCallData(
        tokenContractAddress = tokenContractAddress,
        maxNetworkFee = maxNetworkFee,
    )

    fun getReactivateTokenCallData(
        tokenContractAddress: String,
    ): SmartContractCallData = EthereumYieldLendingReactivateTokenCallData(
        tokenContractAddress = tokenContractAddress,
    )

    fun getEnterCallData(
        tokenContractAddress: String,
    ): SmartContractCallData = EthereumYieldLendingEnterCallData(
        tokenContractAddress = tokenContractAddress
    )

    fun getExitCallData(
        tokenContractAddress: String,
    ): SmartContractCallData = EthereumYieldLendingExitCallData(
        tokenContractAddress = tokenContractAddress
    )
}