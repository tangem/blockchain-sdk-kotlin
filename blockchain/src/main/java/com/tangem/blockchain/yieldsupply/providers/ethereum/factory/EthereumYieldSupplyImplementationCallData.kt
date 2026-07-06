package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that retrieves the current implementation address
 *  from the TangemYieldModuleFactory contract.
 *
 *  Signature: `implementation()`
 */
class EthereumYieldSupplyImplementationCallData : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() = methodId.hexToBytes()

    override fun validate(blockchain: Blockchain): Boolean = true

    companion object {
        const val METHOD_ID = "0x5c60da1b"
        const val dataHex: String = METHOD_ID
    }
}