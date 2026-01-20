package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Token allowance call data in ERC20 - allowance(address,address)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#allowance">EIP-20 Allowance</a>
 */
data class AllowanceERC20TokenCallData(
    private val ownerAddress: String,
    private val spenderAddress: String,
) : Erc20CallData {
    override val methodId = "0xdd62ed3e"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val ownerAddressData = ownerAddress.addressWithoutPrefix().hexToFixedSizeBytes()
            val spenderAddressData = spenderAddress.addressWithoutPrefix().hexToFixedSizeBytes()

            return prefixData + ownerAddressData + spenderAddressData
        }

    override fun validate(): Boolean {
        val addressService = EthereumAddressService()
        return addressService.validate(ownerAddress) && addressService.validate(spenderAddress) &&
            ownerAddress.isNotZeroAddress() && spenderAddress.isNotZeroAddress()
    }
}