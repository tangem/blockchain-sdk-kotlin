package com.tangem.blockchain.yieldsupply.providers.ethereum.registry

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for checking if an address is allowed in SwapExecutionRegistry.
 *
 *  Used for both `allowedSpenders(address)` and `allowedTargets(address)` methods.
 */
class EthereumYieldSupplyAllowedAddressCallData(
    private val address: String,
    override val methodId: String,
) : SmartContractCallData {

    override val data: ByteArray
        get() = methodId.hexToBytes() + address.hexToFixedSizeBytes()

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(address) && address.isNotZeroAddress()
    }

    companion object {
        const val ALLOWED_SPENDERS_METHOD_ID = "0xd8528af0"
        const val ALLOWED_TARGETS_METHOD_ID = "0xb8fe8d5f"

        fun allowedSpenders(address: String) =
            EthereumYieldSupplyAllowedAddressCallData(address, ALLOWED_SPENDERS_METHOD_ID)

        fun allowedTargets(address: String) =
            EthereumYieldSupplyAllowedAddressCallData(address, ALLOWED_TARGETS_METHOD_ID)
    }
}