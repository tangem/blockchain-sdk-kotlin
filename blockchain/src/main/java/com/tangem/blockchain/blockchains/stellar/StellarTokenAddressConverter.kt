package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder.Companion.CONTRACT_ADDRESS_IGNORING_SUFFIX
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder.Companion.STELLAR_SDK_CONTRACT_ADDRESS_SEPARATOR
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder.Companion.TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR

class StellarTokenAddressConverter {

    fun normalizeAddress(address: String?): String? {
        address ?: return address
        val address = address.removeSuffix(CONTRACT_ADDRESS_IGNORING_SUFFIX)
        return address.replace(
            oldValue = STELLAR_SDK_CONTRACT_ADDRESS_SEPARATOR,
            newValue = TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR,
        )
    }
}