package com.tangem.blockchain.yieldsupply.addressfactory

import com.tangem.blockchain.common.Blockchain

/**
 *  Factory to provide Aave contract addresses for Yield Module based on the blockchain.
 */
internal class AaveV3YieldSupplyContractAddressFactory(
    private val blockchain: Blockchain,
) {

    fun getFactoryAddress(): String = when (blockchain) {
        Blockchain.Polygon -> "0x685345d16aA462FB52bDB0D73807a199d1c5Ef76"
        Blockchain.EthereumTestnet -> "0xC013c927B9471889BdECDBd8cFD145CdE8B23f1A"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getProcessorAddress(): String = when (blockchain) {
        Blockchain.Polygon -> "0xA32019c38a7EF45b87c09155600EEc457915b782"
        Blockchain.EthereumTestnet -> "0x54E44c70Ed257Baf81e8CfE77Ffe9c5D2Bf6C04b"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
}