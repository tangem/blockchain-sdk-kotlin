package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.address.ContractAddressValidator
import com.tangem.blockchain.common.address.WalletCoreAddressService

internal class SuiAddressService(
    blockchain: Blockchain,
) : WalletCoreAddressService(blockchain = blockchain), ContractAddressValidator {

    override fun validateContractAddress(address: String): Boolean {
        return validate(address) // Fix in [REDACTED_TASK_KEY]
    }

    override fun reformatContractAddress(address: String?): String? {
        // Ignore coin contract address
        if (address == SuiConstants.COIN_TYPE || address == null) return address

        val parsedAddress = address.split(CONTRACT_ADDRESS_SEPARATOR).toMutableList()
        var rawAddress = parsedAddress.first().removePrefix(HEX_PREFIX)

        // Fill address up to 64 symbols
        while (rawAddress.length < ADDRESS_LENGTH) {
            rawAddress = "0$rawAddress"
        }

        parsedAddress.removeAt(0)
        parsedAddress.add(0, HEX_PREFIX + rawAddress)

        return parsedAddress.joinToString(CONTRACT_ADDRESS_SEPARATOR)
    }

    private companion object {
        const val CONTRACT_ADDRESS_SEPARATOR = "::"
        const val ADDRESS_LENGTH = 64
    }
}