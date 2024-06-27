package com.tangem.blockchain.blockchains.koinos.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/chain/chain.proto>koinos/chain/chain.proto</a>
 */
internal object KoinosChain {

    /**
     * ```protobuf
     * message resource_limit_data {
     *    uint64 disk_storage_limit = 1 [jstype = JS_STRING];
     *    uint64 disk_storage_cost = 2 [jstype = JS_STRING];
     *    uint64 network_bandwidth_limit = 3 [jstype = JS_STRING];
     *    uint64 network_bandwidth_cost = 4 [jstype = JS_STRING];
     *    uint64 compute_bandwidth_limit = 5 [jstype = JS_STRING];
     *    uint64 compute_bandwidth_cost = 6 [jstype = JS_STRING];
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class ResourceLimitData(
        @Json(name = "disk_storage_limit") val diskStorageLimit: Long,
        @Json(name = "disk_storage_cost") val diskStorageCost: Long,
        @Json(name = "network_bandwidth_limit") val networkBandwidthLimit: Long,
        @Json(name = "network_bandwidth_cost") val networkBandwidthCost: Long,
        @Json(name = "compute_bandwidth_limit") val computeBandwidthLimit: Long,
        @Json(name = "compute_bandwidth_cost") val computeBandwidthCost: Long,
    )
}