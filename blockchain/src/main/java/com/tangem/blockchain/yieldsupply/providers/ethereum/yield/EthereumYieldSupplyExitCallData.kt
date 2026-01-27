package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.parseEthereumAddress
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 *  Call data for the method that allows the owner of a specific yield token
 *  to withdraw their funds and deactivate their participation in the protocol associated with that token.
 *
 *  Signature: `withdrawAndDeactivate(address)`
 */
class EthereumYieldSupplyExitCallData(
    val tokenContractAddress: String,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()

            return prefixData + tokenContractAddressData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(tokenContractAddress) && tokenContractAddress.isNotZeroAddress()
    }

    companion object {
        const val METHOD_ID = "0xc65e6dcf"
        fun decode(rawData: String): EthereumYieldSupplyExitCallData? {
            if (!rawData.contains(METHOD_ID)) return null

            val addressService = EthereumAddressService()
            return runCatching {
                val tokenContractAddress = rawData.removePrefix(METHOD_ID).parseEthereumAddress()
                if (addressService.validate(tokenContractAddress)) {
                    EthereumYieldSupplyExitCallData(tokenContractAddress = tokenContractAddress)
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}