package com.tangem.blockchain.blockchains.cardano.network.common.models

internal data class CardanoUnspentOutput(
    val address: String,
    val amount: Long,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val assets: List<Asset>,
) {

    data class Asset(
        val policyID: String,
        val assetNameHex: String,
        val amount: Long,
    )
}