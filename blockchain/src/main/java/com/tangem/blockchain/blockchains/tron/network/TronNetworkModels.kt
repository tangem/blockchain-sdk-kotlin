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
    @Json(name = "address")
    val address: String,
    @Json(name = "visible")
    val visible: Boolean,
)

@JsonClass(generateAdapter = true)
data class TronEnergyFeeData(
    @Json(name = "energyFee")
    val energyFee: Long,
    @Json(name = "sunPerEnergyUnit")
    val sunPerEnergyUnit: Long,
)

@JsonClass(generateAdapter = true)
data class TronGetAccountResponse(
    @Json(name = "balance")
    val balance: Long?,
    // We use [address] field to distinguish this response from
    // an empty JSON that we get if account hasn't been activated
    @Json(name = "address")
    val address: String?,
)

@JsonClass(generateAdapter = true)
data class TronGetAccountResourceResponse(
    @Json(name = "freeNetUsed") val freeNetUsed: Long?,
    @Json(name = "freeNetLimit") val freeNetLimit: Long,
    @Json(name = "EnergyLimit") val energyLimit: Long?,
    @Json(name = "EnergyUsed") val energyUsed: Long?,
)

@JsonClass(generateAdapter = true)
data class TronTransactionInfoRequest(
    @Json(name = "value")
    val value: String,
)

@JsonClass(generateAdapter = true)
data class TronTransactionInfoResponse(
    @Json(name = "id")
    val id: String,
)

@JsonClass(generateAdapter = true)
data class TronChainParametersResponse(
    @Json(name = "chainParameter")
    val chainParameters: List<TronChainParameter>,
)

@JsonClass(generateAdapter = true)
data class TronChainParameter(
    @Json(name = "key")
    val key: String,
    @Json(name = "value")
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
    @Json(name = "number")
    val number: Long,
    @Json(name = "txTrieRoot")
    val txTrieRoot: String,
    @Json(name = "witness_address")
    val witnessAddress: String,
    @Json(name = "parentHash")
    val parentHash: String,
    @Json(name = "version")
    val version: Int,
    @Json(name = "timestamp")
    val timestamp: Long,
)

@JsonClass(generateAdapter = true)
data class TronBroadcastRequest(
    @Json(name = "transaction")
    val transaction: String,
)

@JsonClass(generateAdapter = true)
data class TronBroadcastResponse(
    @Json(name = "result")
    val result: Boolean,
    @Json(name = "txid")
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
    val energyUsed: Long,
)