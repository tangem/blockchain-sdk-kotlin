package com.tangem.blockchain.blockchains.kaspa.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KaspaBalanceResponse(
    var balance: Long? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaUnspentOutputResponse(
    var outpoint: KaspaOutpoint? = null,
    var utxoEntry: KaspaUtxoEntry? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaSendTransactionResponse(
    var transactionId: String? = null,
    var error: String? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaOutpoint(
    var transactionId: String? = null,
    var index: Long? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaUtxoEntry(
    var amount: String? = null,
    var scriptPublicKey: KaspaScriptPublicKeyResponse? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaScriptPublicKeyResponse(
    var scriptPublicKey: String? = null,
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