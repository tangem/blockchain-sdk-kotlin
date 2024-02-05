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
 * @author Andrew Khokhlov on 16/01/2024
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
