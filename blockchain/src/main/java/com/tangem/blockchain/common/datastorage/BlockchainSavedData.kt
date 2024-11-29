package com.tangem.blockchain.common.datastorage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.blockchains.kaspa.krc20.model.Envelope
import java.math.BigDecimal

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

    @JsonClass(generateAdapter = true)
    data class KaspaKRC20IncompleteTokenTransaction(
        @Json(name = "transactionId") val transactionId: String,
        @Json(name = "amountValue") val amountValue: BigDecimal,
        @Json(name = "feeAmountValue") val feeAmountValue: BigDecimal,
        @Json(name = "envelope") val envelope: Envelope,
    ) : BlockchainSavedData
}