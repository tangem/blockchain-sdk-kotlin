package com.tangem.blockchain.blockchains.filecoin.models

/**
 * Filecoin transaction info
 *
 * @property sourceAddress      source address
 * @property destinationAddress destination address
 * @property amount             amount
 * @property nonce              nonce
 *
[REDACTED_AUTHOR]
 */
internal data class FilecoinTxInfo(
    val sourceAddress: String,
    val destinationAddress: String,
    val amount: Long,
    val nonce: Long?,
)