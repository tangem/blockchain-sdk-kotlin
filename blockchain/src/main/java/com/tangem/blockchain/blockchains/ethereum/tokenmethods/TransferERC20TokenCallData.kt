package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.smartcontract.Erc20CallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Token transfer call data in ERC20 - transfer(address,uint256)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#transfer">EIP-20 Transfer</a>
 */
data class TransferERC20TokenCallData(private val destination: String, private val amount: Amount) : Erc20CallData {
    override val methodId: String = "0xa9059cbb"
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = destination.addressWithoutPrefix().hexToFixedSizeBytes()
            val amountData = amount.bigIntegerValue()?.toFixedSizeBytes()
                ?: error("Invalid token transfer amount")
            return prefixData + addressData + amountData
        }

    override fun validate(): Boolean {
        val amountValue = amount.bigIntegerValue()
        val checkAddress = EthereumAddressService().validate(destination) && destination.isNotZeroAddress()
        return checkAddress && amountValue != null
    }
}