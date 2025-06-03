package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.SuiTokenAddressConverter.Companion.ADDRESS_LENGTH_BYTES
import com.tangem.blockchain.blockchains.sui.SuiTokenAddressConverter.Companion.CONTRACT_ADDRESS_PARTS_COUNT
import com.tangem.blockchain.blockchains.sui.SuiTokenAddressConverter.Companion.CONTRACT_ADDRESS_SEPARATOR
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.address.ContractAddressValidator
import com.tangem.blockchain.common.address.WalletCoreAddressService
import com.tangem.common.extensions.hexToBytes

internal class SuiAddressService(
    blockchain: Blockchain,
) : WalletCoreAddressService(blockchain = blockchain), ContractAddressValidator {

    private val tokenAddressConverter = SuiTokenAddressConverter()

    override fun validateContractAddress(address: String): Boolean {
        if (address == SuiConstants.COIN_TYPE) return true
        val formatted = reformatContractAddress(address) ?: return false
        val parts = formatted.split(CONTRACT_ADDRESS_SEPARATOR)
        if (parts.size != CONTRACT_ADDRESS_PARTS_COUNT) return false

        val (objectId, moduleName, structName) = parts

        val objectIdClean = objectId.removePrefix(HEX_PREFIX)
        val objectIdBytes = try {
            objectIdClean.hexToBytes()
        } catch (_: Exception) {
            return false
        }
        if (objectIdBytes.size != ADDRESS_LENGTH_BYTES) return false

        val nameRegex = Regex("""^[_a-zA-Z][_a-zA-Z0-9]*$""")
        if (!nameRegex.matches(moduleName)) return false
        if (!nameRegex.matches(structName)) return false

        return true
    }

    override fun reformatContractAddress(address: String?): String? {
        return tokenAddressConverter.normalizeAddress(address)
    }
}