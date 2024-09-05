package com.tangem.blockchain.blockchains.filecoin.models

/**
 * Filecoin tx gas info
 *
 * @property gasUnitPrice gas unit price
 * @property gasLimit     gas limit
 * @property gasPremium   gas premium
 *
 * @author Andrew Khokhlov on 02/08/2024
 */
data class FilecoinTxGasInfo(
    val gasUnitPrice: Long,
    val gasLimit: Long,
    val gasPremium: Long,
)
