package com.tangem.blockchain.yieldsupply.addressfactory

import com.tangem.blockchain.common.Blockchain

/**
 *  Factory to provide Aave contract addresses for Yield Module based on the blockchain.
 */
internal class AaveV3YieldSupplyContractAddressFactory(
    private val blockchain: Blockchain,
) {

    fun getFactoryAddress(): String = when (blockchain) {
        Blockchain.EthereumTestnet -> "0x62bc085Ef9e7700Af1F572cefCfdf4228E4EA3b8"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getProcessorAddress(): String = when (blockchain) {
        Blockchain.EthereumTestnet -> "0x234D7653Ee1B6d8d87D008e613757Ac2f6Bd5a69"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
}