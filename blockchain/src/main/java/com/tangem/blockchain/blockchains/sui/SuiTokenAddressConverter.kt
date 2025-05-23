package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.HEX_PREFIX

class SuiTokenAddressConverter {

    fun normalizeAddress(address: String?): String? {
        if (address == SuiConstants.COIN_TYPE || address == null) return address

        val parsedAddress = address.split(CONTRACT_ADDRESS_SEPARATOR).toMutableList()
        if (parsedAddress.size != CONTRACT_ADDRESS_PARTS_COUNT) return null
        var rawAddress = parsedAddress.first().removePrefix(HEX_PREFIX)

        while (rawAddress.length < ADDRESS_LENGTH) {
            rawAddress = "0$rawAddress"
        }

        parsedAddress.removeAt(0)
        parsedAddress.add(0, HEX_PREFIX + rawAddress)

        return parsedAddress.joinToString(CONTRACT_ADDRESS_SEPARATOR)
    }

    internal companion object {
        const val CONTRACT_ADDRESS_SEPARATOR = "::"
        const val ADDRESS_LENGTH_BYTES = 32
        const val ADDRESS_LENGTH = ADDRESS_LENGTH_BYTES * 2 // Each byte is represented by 2 hex characters
    }
}