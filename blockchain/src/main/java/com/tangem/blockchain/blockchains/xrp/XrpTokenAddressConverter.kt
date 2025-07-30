package com.tangem.blockchain.blockchains.xrp

class XrpTokenAddressConverter {

    fun normalizeAddress(address: String?): String? {
        address ?: return address
        return address.replace(
            oldValue = "-",
            newValue = XrpTransactionBuilder.Companion.TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR,
        )
    }
}