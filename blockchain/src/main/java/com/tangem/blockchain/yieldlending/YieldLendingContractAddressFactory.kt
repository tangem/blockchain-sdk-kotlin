package com.tangem.blockchain.yieldlending

import com.tangem.blockchain.common.Blockchain

internal class YieldLendingContractAddressFactory(
    private val blockchain: Blockchain,
) {

    fun getFactoryAddress() = when (blockchain) {
        Blockchain.EthereumTestnet -> "0x62bc085Ef9e7700Af1F572cefCfdf4228E4EA3b8"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getProcessorAddress() = when (blockchain) {
        Blockchain.EthereumTestnet -> "0x234D7653Ee1B6d8d87D008e613757Ac2f6Bd5a69"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
}