package com.tangem.blockchain.blockchains.aptos.models

/**
 * Aptos transaction info
 *
 * @property sequenceNumber      sequence number
 * @property publicKey           wallet public key
 * @property sourceAddress       source address
 * @property destinationAddress  destination address
 * @property amount              amount
 * @property gasUnitPrice        gas unit price
 * @property expirationTimestamp expiration timestamp in seconds
 *
[REDACTED_AUTHOR]
 */
data class AptosTransactionInfo(
    val sequenceNumber: Long,
    val publicKey: String,
    val sourceAddress: String,
    val destinationAddress: String,
    val amount: Long,
    val gasUnitPrice: Long,
    val expirationTimestamp: Long,
)