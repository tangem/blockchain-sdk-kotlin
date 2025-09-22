package com.tangem.blockchain.yieldsupply.addressfactory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderType

/**
 *  Factory to provide contract addresses for Yield Module based on the blockchain.
 */
internal class YieldSupplyContractAddressFactory(
    private val blockchain: Blockchain,
) {

    private val aaveV3YieldSupplyContractAddressFactory = AaveV3YieldSupplyContractAddressFactory(blockchain)

    fun isSupported() = when (blockchain) {
        Blockchain.Polygon,
        Blockchain.EthereumTestnet,
        -> true
        else -> false
    }

    fun getYieldSupplyContractAddresses(): YieldSupplyContractAddresses {
        return YieldSupplyContractAddresses(
            factoryContractAddress = aaveV3YieldSupplyContractAddressFactory.getFactoryAddress(),
            processorContractAddress = aaveV3YieldSupplyContractAddressFactory.getProcessorAddress(),
            providerType = YieldSupplyProviderType.AaveV3,
        )
    }
}