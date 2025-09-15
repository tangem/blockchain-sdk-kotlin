package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData

/**
 * Factory to provide [SmartContractCallData] for Yield Module operations.
 */
object YieldSupplyContractCallDataProviderFactory {

    /**
     * Provides call data for deploying a yield supply contract.
     *
     * @param walletAddress The address of the wallet.
     * @param tokenContractAddress The address of the token contract.
     * @param maxNetworkFee The maximum network fee for the transaction.
     * @return [SmartContractCallData] for deploying the yield supply contract.
     */
    fun getDeployCallData(
        walletAddress: String,
        tokenContractAddress: String,
        maxNetworkFee: Amount,
    ): SmartContractCallData = EthereumYieldSupplyDeployCallData(
        address = walletAddress,
        tokenContractAddress = tokenContractAddress,
        maxNetworkFee = maxNetworkFee,
    )

    /**
     * Provides call data for initializing a yield token.
     *
     * @param tokenContractAddress The address of the token contract.
     * @param maxNetworkFee The maximum network fee for the transaction.
     * @return [SmartContractCallData] for initializing the yield token.
     */
    fun getInitTokenCallData(tokenContractAddress: String, maxNetworkFee: Amount): SmartContractCallData =
        EthereumYieldSupplyInitTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )

    /**
     *  Provides call data for reactivating a yield token.
     *
     *  @param tokenContractAddress The address of the token contract.
     *  @param maxNetworkFee The maximum network fee for the transaction.
     *  @return [SmartContractCallData] for reactivating the yield token.
     */
    fun getReactivateTokenCallData(tokenContractAddress: String, maxNetworkFee: Amount): SmartContractCallData =
        EthereumYieldSupplyReactivateTokenCallData(
            tokenContractAddress = tokenContractAddress,
            maxNetworkFee = maxNetworkFee,
        )

    /**
     * Provides call data for entering a yield supply position.
     *
     * @param tokenContractAddress The address of the token contract.
     * @return [SmartContractCallData] for entering the yield supply position.
     */
    fun getEnterCallData(tokenContractAddress: String): SmartContractCallData = EthereumYieldSupplyEnterCallData(
        tokenContractAddress = tokenContractAddress,
    )

    /**
     * Provides call data for exiting a yield supply position.
     *
     * @param tokenContractAddress The address of the token contract.
     * @return [SmartContractCallData] for exiting the yield supply position.
     */
    fun getExitCallData(tokenContractAddress: String): SmartContractCallData = EthereumYieldSupplyExitCallData(
        tokenContractAddress = tokenContractAddress,
    )
}