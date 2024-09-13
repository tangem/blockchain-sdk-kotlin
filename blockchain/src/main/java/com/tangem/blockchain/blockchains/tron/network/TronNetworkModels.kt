package com.tangem.blockchain.blockchains.tron.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Token
import java.math.BigDecimal

data class TronAccountInfo(
    val balance: BigDecimal,
    val tokenBalances: Map<Token, BigDecimal>,
    val confirmedTransactionIds: List<String>,
)

data class TronChainParameters(
    val sunPerEnergyUnit: Long,
    val dynamicEnergyMaxFactor: Long,
    val dynamicIncreaseFactor: Long,
)

@JsonClass(generateAdapter = true)
data class TronGetAccountRequest(
    val address: String,
    val visible: Boolean,
)

@JsonClass(generateAdapter = true)
data class TronEnergyFeeData(
    val energyFee: Int,
    val sunPerEnergyUnit: Long,
)

@JsonClass(generateAdapter = true)
data class TronGetAccountResponse(
    val balance: Long?,
    // We use [address] field to distinguish this response from
    // an empty JSON that we get if account hasn't been activated
    val address: String?,
)

@JsonClass(generateAdapter = true)
data class TronGetAccountResourceResponse(
    val freeNetUsed: Int?,
    val freeNetLimit: Int,
    @Json(name = "EnergyLimit") val energyLimit: BigDecimal,
    @Json(name = "EnergyUsed") val energyUsed: BigDecimal?,
)

@JsonClass(generateAdapter = true)
data class TronTransactionInfoRequest(
    val value: String,
)

@JsonClass(generateAdapter = true)
data class TronTransactionInfoResponse(
    val id: String,
)

@JsonClass(generateAdapter = true)
data class TronChainParametersResponse(
    @Json(name = "chainParameter")
    val chainParameters: List<TronChainParameter>,
)

@JsonClass(generateAdapter = true)
data class TronChainParameter(
    val key: String,
    val value: Long? = null,
)

@JsonClass(generateAdapter = true)
data class TronBlock(
    @Json(name = "block_header")
    val blockHeader: BlockHeader,
)

@JsonClass(generateAdapter = true)
data class BlockHeader(
    @Json(name = "raw_data")
    val rawData: RawData,
)

@JsonClass(generateAdapter = true)
data class RawData(
    val number: Long,
    val txTrieRoot: String,
    @Json(name = "witness_address")
    val witnessAddress: String,
    val parentHash: String,
    val version: Int,
    val timestamp: Long,
)

@JsonClass(generateAdapter = true)
data class TronBroadcastRequest(
    val transaction: String,
)

@JsonClass(generateAdapter = true)
data class TronBroadcastResponse(
    val result: Boolean,
    val txid: String,
    @Json(name = "message")
    val errorMessage: String?,
)

@JsonClass(generateAdapter = true)
data class TronTriggerSmartContractRequest(
    @Json(name = "owner_address")
    val ownerAddress: String,

    @Json(name = "contract_address")
    val contractAddress: String,

    @Json(name = "function_selector")
    val functionSelector: String,

    @Json(name = "fee_limit")
    val feeLimit: Long? = null,

    @Json(name = "parameter")
    val parameter: String,

    @Json(name = "visible")
    val visible: Boolean,
)

@JsonClass(generateAdapter = true)
data class TronTriggerSmartContractResponse(
    @Json(name = "constant_result")
    val constantResult: List<String>,
    @Json(name = "energy_used")
    val energyUsed: Int,
)