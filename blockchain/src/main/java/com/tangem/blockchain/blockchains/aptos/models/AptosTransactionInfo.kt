package com.tangem.blockchain.blockchains.aptos.models

/**
 * Aptos transaction info
 *
 * @property sequenceNumber      sequence number
 * @property publicKey           wallet public key
 * @property sourceAddress       source address
 * @property destinationAddress  destination address
 * @property amount              amount
 * @property contractAddress     contract address, if null then it is a native coin
 * @property gasUnitPrice        gas unit price
 * @property maxGasAmount        max gas amount
 * @property expirationTimestamp expiration timestamp in seconds
 * @property hash                hash of transaction
 *
 * @author Andrew Khokhlov on 16/01/2024
 */
data class AptosTransactionInfo(
    val sequenceNumber: Long,
    val publicKey: String,
    val sourceAddress: String,
    val destinationAddress: String,
    val amount: Long,
    val contractAddress: String? = null,
    val gasUnitPrice: Long,
    val maxGasAmount: Long,
    val expirationTimestamp: Long,
    val hash: String? = null,
)
