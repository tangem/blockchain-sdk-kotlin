package com.tangem.blockchain.blockchains.cardano.utils

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.extensions.decodeBech32
import com.tangem.blockchain.extensions.isValidHex

internal object CardanoContractAddressRecognizer {

    private const val POLICY_ID_LENGTH = 56

    fun recognize(address: String): Address? {
        val isPrefixValid = address.startsWith(
            prefix = CardanoUtils.CARDANO_FINGERPRINT_ADDRESS_PREFIX,
        )

        if (isPrefixValid && address.decodeBech32() != null) return Address.Fingerprint(address)

        if (!address.isValidHex()) return null

        return when {
            address.length == POLICY_ID_LENGTH -> Address.PolicyID(address)
            address.length > POLICY_ID_LENGTH -> Address.AssetID(address)
            else -> null
        }
    }

    sealed interface Address {

        val value: String

        data class PolicyID(override val value: String) : Address

        data class AssetID(override val value: String) : Address

        data class Fingerprint(override val value: String) : Address
    }
}