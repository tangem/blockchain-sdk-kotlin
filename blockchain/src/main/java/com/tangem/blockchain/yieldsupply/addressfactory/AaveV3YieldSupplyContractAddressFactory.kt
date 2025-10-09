package com.tangem.blockchain.yieldsupply.addressfactory

import com.tangem.blockchain.common.Blockchain

/**
 *  Factory to provide Aave contract addresses for Yield Module based on the blockchain.
 */
internal class AaveV3YieldSupplyContractAddressFactory(
    private val blockchain: Blockchain,
) {

    fun getFactoryAddress(): String = when (blockchain) {
        Blockchain.Polygon -> "0x1bE509C2fF23dF065E15A6d37b0eFe4c839c62fE"
        Blockchain.EthereumTestnet -> "0xF3b31452E8EE5B294D7172B69Bd02decF2255FCd"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getProcessorAddress(): String = when (blockchain) {
        Blockchain.Polygon -> "0xD021F1D410aCB895aB110a0CbB740a33db209bDD"
        Blockchain.EthereumTestnet -> "0x9A4b70A216C1A84d72a490f8cD3014Fdb538d249"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
}