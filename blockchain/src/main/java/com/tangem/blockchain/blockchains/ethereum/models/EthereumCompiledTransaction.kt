package com.tangem.blockchain.blockchains.ethereum.models

import kotlinx.serialization.Serializable
import shadow.com.google.gson.annotations.SerializedName

/**
 * Model of filled Ethereum transaction used in staking
 */
@Serializable
data class EthereumCompiledTransaction(
    @SerializedName("from")
    val from: String,
    @SerializedName("gasLimit")
    val gasLimit: String,
    @SerializedName("value")
    val value: String?,
    @SerializedName("to")
    val to: String,
    @SerializedName("data")
    val data: String,
    @SerializedName("nonce")
    val nonce: Int,
    @SerializedName("type")
    val type: Int,
    @SerializedName("gasPrice")
    val gasPrice: String?,
    @SerializedName("maxFeePerGas")
    val maxFeePerGas: String?,
    @SerializedName("maxPriorityFeePerGas")
    val maxPriorityFeePerGas: String?,
    @SerializedName("chainId")
    val chainId: Int,
)