package com.tangem.blockchain.blockchains.bitcoin.walletconnect.models

/**
 * Request model for WalletConnect sendTransfer method.
 *
 * @property account Default (SegWit) address of the wallet (for multi-address: m/84'/0'/0'/0/0)
 * @property recipientAddress Recipient's address
 * @property amount Amount to send in satoshis (minimum BTC unit)
 * @property changeAddress Optional custom change address (must be one of wallet's addresses)
 * @property memo Optional memo as hex string (without 0x prefix, max 80 bytes when decoded)
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#sendtransfer">sendTransfer Documentation</a>
 */
data class SendTransferRequest(
    val account: String,
    val recipientAddress: String,
    val amount: String,
    val changeAddress: String? = null,
    val memo: String? = null,
)

/**
 * Response model for WalletConnect sendTransfer method.
 *
 * @property txid Transaction hash as hex string (without 0x prefix)
 */
data class SendTransferResponse(
    val txid: String,
)

/**
 * Address intention type for Bitcoin addresses.
 */
enum class AddressIntention {
    PAYMENT,
    ORDINAL,
    ;

    companion object {
        fun fromString(value: String): AddressIntention? = when (value.lowercase()) {
            "payment" -> PAYMENT
            "ordinal" -> ORDINAL
            else -> null
        }
    }

    fun toApiString(): String = name.lowercase()
}

/**
 * Request model for WalletConnect getAccountAddresses method.
 *
 * @property account Default (SegWit) address of the wallet
 * @property intentions Optional filter for address types ("payment", "ordinal", or both)
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#getaccountaddresses">getAccountAddresses Documentation</a>
 */
data class GetAccountAddressesRequest(
    val account: String,
    val intentions: List<String>? = null,
)

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
 * Response model for WalletConnect getAccountAddresses method.
 *
 * @property addresses List of wallet addresses with metadata
 */
data class GetAccountAddressesResponse(
    val addresses: List<AccountAddress>,
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
 * Request model for WalletConnect signPsbt method.
 *
 * @property psbt PSBT transaction in Base64 encoding
 * @property signInputs List of inputs to sign
 * @property broadcast Whether to broadcast the transaction after signing (default: false)
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#signpsbt">signPsbt Documentation</a>
 */
data class SignPsbtRequest(
    val psbt: String,
    val signInputs: List<SignInput>,
    val broadcast: Boolean? = false,
)

/**
 * Response model for WalletConnect signPsbt method.
 *
 * @property psbt Signed PSBT transaction in Base64 encoding
 * @property txid Optional transaction hash (required if broadcast=true)
 */
data class SignPsbtResponse(
    val psbt: String,
    val txid: String? = null,
)

/**
 * Message signing protocol type.
 */
enum class SignMessageProtocol {
    ECDSA,
    BIP322,
    ;

    companion object {
        fun fromString(value: String): SignMessageProtocol = when (value.lowercase()) {
            "ecdsa" -> ECDSA
            "bip322" -> BIP322
            else -> ECDSA // Default to ECDSA
        }
    }
}

/**
 * Request model for WalletConnect signMessage method.
 *
 * @property account The connected account's first external address
 * @property message Message to sign
 * @property address Optional address to sign with (if different from account)
 * @property protocol Optional signing protocol ("ecdsa" or "bip322", default: "ecdsa")
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc#signmessage">signMessage Documentation</a>
 */
data class SignMessageRequest(
    val account: String,
    val message: String,
    val address: String? = null,
    val protocol: String? = "ecdsa",
)

/**
 * Response model for WalletConnect signMessage method.
 *
 * @property address Address used for signing
 * @property signature Signature as hex string (without 0x prefix)
 * @property messageHash Optional message hash as hex string (without 0x prefix)
 */
data class SignMessageResponse(
    val address: String,
    val signature: String,
    val messageHash: String? = null,
)