package com.tangem.blockchain.yieldsupply.addressfactory

import com.tangem.blockchain.yieldsupply.YieldSupplyProviderType

/**
 *  Data class to hold contract addresses and provider type for Yield Module.
 */
data class YieldSupplyContractAddresses(
    val factoryContractAddress: String,
    val processorContractAddress: String,
    val providerType: YieldSupplyProviderType,
)