package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigInteger

/**
 * Response model for eth_getTransactionReceipt
 * @see <a href="https://ethereum.org/developers/docs/apis/json-rpc/#eth_gettransactionreceipt">Ethereum JSON-RPC API</a>
 */
@JsonClass(generateAdapter = true)
internal data class EthereumTransactionReceiptResponse(
    @Json(name = "transactionHash") val transactionHash: String,
    @Json(name = "blockNumber") val blockNumber: String?,
    @Json(name = "status") val status: String?, // "0x1" on success, "0x0" when the transaction reverted
    @Json(name = "gasUsed") val gasUsed: String?,
    @Json(name = "effectiveGasPrice") val effectiveGasPrice: String?,
) {
    fun toDomain(): EthereumTransactionReceipt = EthereumTransactionReceipt(
        transactionHash = transactionHash,
        isSuccess = status == SUCCESS_STATUS,
        blockNumber = blockNumber.hexToBigIntegerOrNull(),
        gasUsed = gasUsed.hexToBigIntegerOrNull(),
    )

    private fun String?.hexToBigIntegerOrNull(): BigInteger? = this?.removePrefix("0x")?.toBigIntegerOrNull(radix = 16)

    private companion object {
        const val SUCCESS_STATUS = "0x1"
    }
}

/**
 * Outcome of a mined transaction.
 *
 * A receipt only exists once the transaction has been included in a block, so obtaining one is itself the
 * proof that the transaction settled. [isSuccess] then distinguishes a successful transaction from a
 * reverted one — a distinction `eth_getTransactionByHash` cannot make, since a reverted transaction also
 * carries a block number.
 */
data class EthereumTransactionReceipt(
    val transactionHash: String,
    val isSuccess: Boolean,
    val blockNumber: BigInteger?,
    val gasUsed: BigInteger?,
)