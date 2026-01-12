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

    @JsonClass(generateAdapter = true)
    data class Trustline(
        @Json(name = "createdTrustline") val createdTrustline: Set<String> = emptySet(),
        @Json(name = "trustlinesWithoutNoRipple") val trustlinesWithoutNoRipple: Set<String> = emptySet(),
    ) : BlockchainSavedData

    @JsonClass(generateAdapter = true)
    data class YieldSupplyModule(
        @Json(name = "contractAddress") val yieldContractAddress: String? = null,
    ) : BlockchainSavedData

    @JsonClass(generateAdapter = true)
    data class QuaiDerivationIndex(
        @Json(name = "index") val index: Int,
    ) : BlockchainSavedData

    @JsonClass(generateAdapter = true)
    data class PendingTransactions(
        @Json(name = "transactions") val transactions: List<PendingTransaction> = emptyList(),
    ) : BlockchainSavedData
}

/**
 * Represents a pending transaction stored in persistent storage.
 *
 * @property transactionId Transaction hash (hex string)
 * @property blockchain Blockchain identifier (e.g., "ETH", "POLYGON")
 * @property walletPublicKey Wallet public key (hex string)
 * @property providerName Provider name (null for public providers, specific name for private providers like "Blink")
 * @property sentAt Timestamp when transaction was sent (milliseconds since epoch)
 */
@JsonClass(generateAdapter = true)
internal data class PendingTransaction(
    @Json(name = "transactionId") val transactionId: String,
    @Json(name = "blockchain") val blockchain: String,
    @Json(name = "providerName") val providerName: String? = null,
    @Json(name = "sentAt") val sentAt: Long,
    @Json(name = "contractAddress") val contractAddress: String? = null,
)