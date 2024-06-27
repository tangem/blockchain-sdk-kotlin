package com.tangem.blockchain.common.datastorage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

internal sealed interface BlockchainSavedData {

    /**
     * @property isCacheCleared is used to clear cache one time for all users, to prevent account duplicates. More
     * details inside AND-7008
     */
    @JsonClass(generateAdapter = true)
    data class Hedera(
        @Json(name = "accountId") val accountId: String,
        @Json(name = "associatedTokens") val associatedTokens: Set<String> = emptySet(),
        // TODO: Remove this flag in future https://tangem.atlassian.net/browse/AND-7025
        @Json(name = "cache_cleared") val isCacheCleared: Boolean = false,
    ) : BlockchainSavedData
}
