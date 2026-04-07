package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.*
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus

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

    /**
     * Wraps the given call data with an upgrade-and-call transaction if the module version is outdated.
     *
     * @param versionStatus The current version status of the yield module.
     * @param originalCallData The original call data to wrap.
     * @return The original call data if the module is up-to-date,
     *         or wrapped call data with [EthereumYieldSupplyUpgradeToAndCallCallData] if an upgrade is available.
     * @throws IllegalStateException if the module is outdated but cannot be upgraded.
     */
    fun wrapWithUpgradeIfNeeded(
        versionStatus: YieldModuleVersionStatus,
        originalCallData: SmartContractCallData,
    ): SmartContractCallData = when (versionStatus) {
        is YieldModuleVersionStatus.UpToDate,
        is YieldModuleVersionStatus.NotDeployed,
        -> originalCallData
        is YieldModuleVersionStatus.UpgradeAvailable -> EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = versionStatus.latestImplementation,
            callData = originalCallData.data,
        )
        is YieldModuleVersionStatus.UpgradeUnavailable -> throw YieldModuleUpgradeUnavailableException(
            currentImplementation = versionStatus.currentImplementation,
        )
    }

    /**
     * Provides call data for sending amount from yield module
     *
     * @param tokenContractAddress The address of the token contract.
     * @param destinationAddress Destination address.
     * @param amount Sending amount.
     *
     * @return [SmartContractCallData] for sending while in yield supply.
     */
    fun getSendCallData(
        tokenContractAddress: String,
        destinationAddress: String,
        amount: Amount,
    ): SmartContractCallData = EthereumYieldSupplySendCallData(
        tokenContractAddress = tokenContractAddress,
        destinationAddress = destinationAddress,
        amount = amount,
    )
}