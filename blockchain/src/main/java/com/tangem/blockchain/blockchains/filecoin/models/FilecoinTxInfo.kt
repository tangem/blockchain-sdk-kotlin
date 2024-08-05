package com.tangem.blockchain.blockchains.filecoin.models

/**
 * Filecoin transaction info
 *
 * @property sourceAddress      source address
 * @property destinationAddress destination address
 * @property amount             amount
 * @property nonce              nonce
 *
 * @author Andrew Khokhlov on 29/07/2024
 */
internal data class FilecoinTxInfo(
    val sourceAddress: String,
    val destinationAddress: String,
    val amount: Long,
    val nonce: Long?,
)
