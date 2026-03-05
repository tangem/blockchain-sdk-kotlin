package com.tangem.blockchain.blockchains.bitcoin.walletconnect.models

/**
 * Address intention type for Bitcoin addresses.
 */
enum class AddressIntention(private val apiValue: String) {
    PAYMENT("payment"),
    ORDINAL("ordinal"),
    ;

    fun toApiString(): String = apiValue

    companion object {
        fun fromString(value: String): AddressIntention? = entries.find {
            it.apiValue.equals(value, ignoreCase = true)
        }
    }
}

/**
 * Single address entry in getAccountAddresses response.
 *
 * @property address Bitcoin address
 * @property publicKey Optional public key as hex string (without 0x prefix)
 * @property path Optional derivation path (e.g., "m/84'/0'/0'/0/0")
 * @property intention Optional address purpose ("payment" or "ordinal")
 */
data class AccountAddress(
    val address: String,
    val publicKey: String? = null,
    val path: String? = null,
    val intention: String? = null,
)

/**
 * Input specification for PSBT signing.
 *
 * @property address Address whose private key should be used for signing
 * @property index Index of the input to sign
 * @property sighashTypes Optional sighash flags (default: [1] for SIGHASH_ALL)
 */
data class SignInput(
    val address: String,
    val index: Int,
    val sighashTypes: List<Int>? = null,
)

/**
 * Message signing protocol type.
 */
enum class SignMessageProtocol(private val apiValue: String) {
    ECDSA("ecdsa"),
    BIP322("bip322"),
    ;

    fun toApiString(): String = apiValue

    companion object {
        private val DEFAULT = ECDSA

        fun fromString(value: String): SignMessageProtocol = entries.find {
            it.apiValue.equals(value, ignoreCase = true)
        } ?: DEFAULT
    }
}