package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder.Companion.OUR_BACKEND_CONTRACT_ADDRESS_SEPARATOR
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder.Companion.STELLAR_SDK_CONTRACT_ADDRESS_SEPARATOR
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.ContractAddressValidator
import com.tangem.common.card.EllipticCurve
import org.stellar.sdk.KeyPair

class StellarAddressService : AddressService(), ContractAddressValidator {
    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        val kp = KeyPair.fromPublicKey(walletPublicKey)
        return kp.accountId
    }

    override fun validate(address: String): Boolean {
        return try {
            KeyPair.fromAccountId(address) != null
        } catch (exception: Exception) {
            false
        }
    }

    override fun validateContractAddress(address: String): Boolean {
        var split = address.split(OUR_BACKEND_CONTRACT_ADDRESS_SEPARATOR)
        if (split.size != 2) split = address.split(STELLAR_SDK_CONTRACT_ADDRESS_SEPARATOR)
        val currencyCode = split.getOrNull(0) ?: return false
        val issuer = split.getOrNull(1) ?: return false
        return isValidCurrencyCode(currencyCode) && validate(issuer)
    }

    private fun isValidCurrencyCode(code: String): Boolean {
        val regex = Regex("^[A-Z0-9]{1,12}$")
        return regex.matches(code)
    }
}