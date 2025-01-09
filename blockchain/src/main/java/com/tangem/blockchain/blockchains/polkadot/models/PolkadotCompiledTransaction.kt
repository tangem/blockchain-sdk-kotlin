package com.tangem.blockchain.blockchains.polkadot.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model of filled Polkadot transaction used in staking
 */
@JsonClass(generateAdapter = true)
internal data class PolkadotCompiledTransaction(
    @Json(name = "tx")
    val tx: Inner,
    @Json(name = "specName")
    val specName: String,
    @Json(name = "specVersion")
    val specVersion: String,
    @Json(name = "metadataRpc")
    val metadataRpc: String,
) {
    @JsonClass(generateAdapter = true)
    data class Inner(
        @Json(name = "address")
        val address: String,
        @Json(name = "blockHash")
        val blockHash: String,
        @Json(name = "blockNumber")
        val blockNumber: String,
        @Json(name = "era")
        val era: String,
        @Json(name = "genesisHash")
        val genesisHash: String,
        @Json(name = "metadataRpc")
        val metadataRpc: String,
        @Json(name = "method")
        val method: String,
        @Json(name = "nonce")
        val nonce: String,
        @Json(name = "signedExtensions")
        val signedExtensions: List<String>,
        @Json(name = "specVersion")
        val specVersion: String,
        @Json(name = "tip")
        val tip: String,
        @Json(name = "transactionVersion")
        val transactionVersion: String,
        @Json(name = "version")
        val version: Int,
    )
}