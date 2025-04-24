package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Token balance call data in ERC20 - balanceOf(address)
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-20#balanceof">EIP-20 Balance Of</a>
 */
data class TokenBalanceERC20TokenCallData(
    private val address: String,
) : SmartContractCallData {

    override val methodId: String = "0x70a08231"

    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val addressData = address.hexToFixedSizeBytes()
            return prefixData + addressData
        }
}