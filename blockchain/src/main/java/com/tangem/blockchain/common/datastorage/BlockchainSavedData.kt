package com.tangem.blockchain.common.datastorage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

internal sealed interface BlockchainSavedData {

    /**
     * @property isCacheCleared is used to clear cache one time for all users, to prevent account duplicates. More
     * details inside [REDACTED_TASK_KEY]
     */
    @JsonClass(generateAdapter = true)
    data class Hedera(
        @Json(name = "accountId") val accountId: String,
        @Json(name = "associatedTokens") val associatedTokens: Set<String> = emptySet(),
        // TODO: Remove this flag in future [REDACTED_JIRA]
        @Json(name = "cache_cleared") val isCacheCleared: Boolean = false,
    ) : BlockchainSavedData
}