package com.tangem.blockchain.blockchains.ethereum.tokenmethods

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray

/**
 * Reverse resolve ENS address call data for Universal Resolver
 * Method: reverse(bytes memory encodedAddress, uint256 coinType)
 *
 * @see <a href="https://docs.ens.domains/resolvers/universal">ENS Universal Resolver Documentation</a>
 */
internal data class ReverseResolveENSAddressCallData(
    private val address: ByteArray,
    private val coinType: Int = 60, // Ethereum mainnet
) : SmartContractCallData {

    override val methodId: String = "0x5d78a217"

    override val data: ByteArray
        get() {
            val addressOffset = 64.toByteArray().toFixedSizeBytes() // offset to bytes
            val coinTypeData = coinType.toByteArray().toFixedSizeBytes() // chain id (ethereum 60)
            val addressLength = address.size.toByteArray().toFixedSizeBytes() // bytes length
            val addressData = address // address bytes without padding

            return methodId.hexToBytes() +
                addressOffset +
                coinTypeData +
                addressLength +
                addressData
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReverseResolveENSAddressCallData

        if (!address.contentEquals(other.address)) return false
        if (coinType != other.coinType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.contentHashCode()
        result = 31 * result + coinType
        return result
    }

    override fun validate(): Boolean {
        return address.isNotEmpty()
    }
}