package com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// region Extrinsics List
@JsonClass(generateAdapter = true)
internal data class ExtrinsicsListBody(
    @Json(name = "address") val address: String,
    @Json(name = "after_id") val afterExtrinsicId: Long?,
    @Json(name = "order") val order: String,
    @Json(name = "page") val page: Int,
    @Json(name = "row") val row: Int,
)

@JsonClass(generateAdapter = true)
data class ExtrinsicListResponse(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "extrinsics") val extrinsic: List<ExtrinsicListItemResponse>? = null,
)

@JsonClass(generateAdapter = true)
data class ExtrinsicListItemResponse(
    @Json(name = "extrinsic_hash") val hash: String? = null,
    @Json(name = "id") val id: Long? = null,
)
// endregion

// region Extrinsic Detail
@JsonClass(generateAdapter = true)
internal data class ExtrinsicDetailBody(
    @Json(name = "hash") val hash: String,
)

@JsonClass(generateAdapter = true)
data class ExtrinsicDetailResponse(
    @Json(name = "lifetime") val lifetime: ExtrinsicDetailLifetimeResponse? = null,
)

@JsonClass(generateAdapter = true)
data class ExtrinsicDetailLifetimeResponse(
    @Json(name = "birth") val birth: Int? = null,
    @Json(name = "death") val death: Int? = null,
)
// endregion

//region Account
@JsonClass(generateAdapter = true)
internal data class AccountBody(
    @Json(name = "key") val key: String,
)

@JsonClass(generateAdapter = true)
data class AccountResponse(
    @Json(name = "account") val account: AccountInfoResponse? = null,
)

@JsonClass(generateAdapter = true)
data class AccountInfoResponse(
    @Json(name = "address") val address: String? = null,
    @Json(name = "nonce") val nonce: Int? = null,
    @Json(name = "count_extrinsic") val countExtrinsic: Int? = null,
)
//endregion

@JsonClass(generateAdapter = true)
data class PolkadotAccountHealthCheckResponse<Result>(
    @Json(name = "data") val data: Result? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
)