package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the `factory()` getter on a deployed Yield Module proxy.
 *
 *  Returns the factory that deployed the module and governs its upgrade authorization on-chain
 *  (`_authorizeUpgrade` -> `factory.isValidImplementation(newImplementation)`). This reference is
 *  immutable and may differ from the currently configured factory for older-generation modules.
 *
 *  Signature: `factory()`
 */
class EthereumYieldSupplyModuleFactoryCallData : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() = methodId.hexToBytes()

    override fun validate(blockchain: Blockchain): Boolean = true

    companion object {
        const val METHOD_ID = "0xc45a0155"
        const val dataHex: String = METHOD_ID
    }
}