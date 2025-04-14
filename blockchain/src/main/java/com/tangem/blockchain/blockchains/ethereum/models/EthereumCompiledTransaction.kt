package com.tangem.blockchain.blockchains.ethereum.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model of filled Ethereum transaction used in staking
 */
@JsonClass(generateAdapter = true)
data class EthereumCompiledTransaction(
    @Json(name = "from")
    val from: String,
    @Json(name = "gasLimit")
    val gasLimit: String,
    @Json(name = "value")
    val value: String?,
    @Json(name = "to")
    val to: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "nonce")
    val nonce: Int,
    @Json(name = "type")
    val type: Int,
    @Json(name = "gasPrice")
    val gasPrice: String?,
    @Json(name = "maxFeePerGas")
    val maxFeePerGas: String?,
    @Json(name = "maxPriorityFeePerGas")
    val maxPriorityFeePerGas: String?,
    @Json(name = "chainId")
    val chainId: Int,
)