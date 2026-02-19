package com.tangem.blockchain.transactionhistory.blockchains.solana.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SolanaRpcResponse<T>(
    @Json(name = "result") val result: T?,
    @Json(name = "error") val error: SolanaRpcError?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaRpcError(
    @Json(name = "code") val code: Int?,
    @Json(name = "message") val message: String?,
)

// region getSignaturesForAddress response

@JsonClass(generateAdapter = true)
internal data class SolanaSignatureInfo(
    @Json(name = "signature") val signature: String,
    @Json(name = "blockTime") val blockTime: Long?,
    @Json(name = "confirmationStatus") val confirmationStatus: String?,
    @Json(name = "err") val err: Any?,
)

// endregion

// region getTransaction (jsonParsed) response

@JsonClass(generateAdapter = true)
internal data class SolanaTransactionResponse(
    @Json(name = "blockTime") val blockTime: Long?,
    @Json(name = "meta") val meta: SolanaTransactionMeta?,
    @Json(name = "transaction") val transaction: SolanaTransactionData?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaTransactionMeta(
    @Json(name = "err") val err: Any?,
    @Json(name = "fee") val fee: Long,
    @Json(name = "preBalances") val preBalances: List<Long>,
    @Json(name = "postBalances") val postBalances: List<Long>,
    @Json(name = "preTokenBalances") val preTokenBalances: List<SolanaTokenBalance>?,
    @Json(name = "postTokenBalances") val postTokenBalances: List<SolanaTokenBalance>?,
    @Json(name = "innerInstructions") val innerInstructions: List<SolanaInnerInstruction>?,
    @Json(name = "rewards") val rewards: List<Any>?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaTokenBalance(
    @Json(name = "accountIndex") val accountIndex: Int,
    @Json(name = "mint") val mint: String,
    @Json(name = "owner") val owner: String?,
    @Json(name = "uiTokenAmount") val uiTokenAmount: SolanaTokenAmount?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaTokenAmount(
    @Json(name = "amount") val amount: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "uiAmount") val uiAmount: Double?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaInnerInstruction(
    @Json(name = "index") val index: Int,
    @Json(name = "instructions") val instructions: List<SolanaInstruction>,
)

@JsonClass(generateAdapter = true)
internal data class SolanaTransactionData(
    @Json(name = "message") val message: SolanaTransactionMessage?,
    @Json(name = "signatures") val signatures: List<String>,
)

@JsonClass(generateAdapter = true)
internal data class SolanaTransactionMessage(
    @Json(name = "accountKeys") val accountKeys: List<SolanaAccountKey>,
    @Json(name = "instructions") val instructions: List<SolanaInstruction>,
)

@JsonClass(generateAdapter = true)
internal data class SolanaAccountKey(
    @Json(name = "pubkey") val pubkey: String,
    @Json(name = "signer") val signer: Boolean?,
    @Json(name = "writable") val writable: Boolean?,
    @Json(name = "source") val source: String?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaInstruction(
    @Json(name = "programId") val programId: String?,
    @Json(name = "program") val program: String?,
    @Json(name = "parsed") val parsed: SolanaParsedInstruction?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaParsedInstruction(
    @Json(name = "type") val type: String?,
    @Json(name = "info") val info: SolanaInstructionInfo?,
)

@JsonClass(generateAdapter = true)
internal data class SolanaInstructionInfo(
    @Json(name = "source") val source: String?,
    @Json(name = "destination") val destination: String?,
    @Json(name = "lamports") val lamports: Long?,
    @Json(name = "amount") val amount: String?,
    @Json(name = "authority") val authority: String?,
    @Json(name = "tokenAmount") val tokenAmount: SolanaTokenAmount?,
    @Json(name = "mint") val mint: String?,
)

// endregion