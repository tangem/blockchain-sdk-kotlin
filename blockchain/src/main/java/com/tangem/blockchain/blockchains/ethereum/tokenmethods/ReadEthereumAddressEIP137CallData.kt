package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytesRightPadding
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray

/**
 * Read Ethereum address call data in EIP-137 - `eth_getCode(address)`
 */
internal data class ReadEthereumAddressEIP137CallData(
    private val nameBytes: ByteArray,
    private val callDataBytes: ByteArray,
) : SmartContractCallData {

    override val methodId: String = "0x9061b923"

    override val data: ByteArray
        get() {
            val nameBytesOffset = 64.toByteArray().toFixedSizeBytes()
            val callDataBytesOffset = 128.toByteArray().toFixedSizeBytes()
            val nameBytesLength = nameBytes.size.toByteArray().toFixedSizeBytes()
            val nameBytesSized32 = nameBytes.toFixedSizeBytesRightPadding()

            val callDataBytesLength = callDataBytes.size.toByteArray().toFixedSizeBytes()

            return methodId.hexToBytes() +
                nameBytesOffset +
                callDataBytesOffset +
                nameBytesLength +
                nameBytesSized32 +
                callDataBytesLength +
                callDataBytes
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return nameBytes.isNotEmpty() && callDataBytes.isNotEmpty()
    }
}