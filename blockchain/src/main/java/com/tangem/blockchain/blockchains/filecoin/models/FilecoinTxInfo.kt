package com.tangem.blockchain.blockchains.filecoin.models

import java.math.BigInteger

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
    val amount: BigInteger,
    val nonce: Long?,
)