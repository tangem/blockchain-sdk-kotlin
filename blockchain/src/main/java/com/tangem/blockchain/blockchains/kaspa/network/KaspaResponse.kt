package com.tangem.blockchain.blockchains.kaspa.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KaspaBalanceResponse(
    @Json(name = "balance")
    val balance: Long? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaUnspentOutputResponse(
    @Json(name = "outpoint")
    val outpoint: KaspaOutpoint? = null,
    @Json(name = "utxoEntry")
    val utxoEntry: KaspaUtxoEntry? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaSendTransactionResponse(
    @Json(name = "transactionId")
    val transactionId: String? = null,
    @Json(name = "error")
    val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaOutpoint(
    @Json(name = "transactionId")
    val transactionId: String? = null,
    @Json(name = "index")
    val index: Long? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaUtxoEntry(
    @Json(name = "amount")
    val amount: String? = null,
    @Json(name = "scriptPublicKey")
    val scriptPublicKey: KaspaScriptPublicKeyResponse? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaScriptPublicKeyResponse(
    @Json(name = "scriptPublicKey")
    val scriptPublicKey: String? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaFeeBucketResponse(
    @Json(name = "feerate") val feeRate: Long,
    @Json(name = "estimatedSeconds") val estimatedSeconds: Double,
)

@JsonClass(generateAdapter = true)
data class KaspaFeeEstimateResponse(
    @Json(name = "priorityBucket") val priorityBucket: KaspaFeeBucketResponse,
    @Json(name = "normalBuckets") val normalBuckets: List<KaspaFeeBucketResponse>,
    @Json(name = "lowBuckets") val lowBuckets: List<KaspaFeeBucketResponse>,
)

@JsonClass(generateAdapter = true)
data class KaspaMassResponse(
    @Json(name = "mass") val mass: Long,
    @Json(name = "storage_mass") val storageMass: Long,
    @Json(name = "compute_mass") val computeMass: Long,
)