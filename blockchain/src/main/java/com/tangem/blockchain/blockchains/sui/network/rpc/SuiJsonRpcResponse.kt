package com.tangem.blockchain.blockchains.sui.network.rpc

import com.squareup.moshi.Json
import java.math.BigDecimal

internal data class SuiJsonRpcResponse<T : Any>(
    @Json(name = "jsonrpc") val jsonRpc: String,
    @Json(name = "id") val id: String,
    @Json(name = "result") val result: T?,
    @Json(name = "error") val error: SuiJsonRpcError?,
)

internal data class SuiJsonRpcError(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
)

internal data class SuiCoinsResponse(
    @Json(name = "data") val data: List<Data>,
    @Json(name = "hasNextPage") val hasNextPage: Boolean,
    @Json(name = "nextCursor") val nextCursor: String?,
) {

    data class Data(
        @Json(name = "balance") val balance: String,
        @Json(name = "coinObjectId") val coinObjectId: String,
        @Json(name = "coinType") val coinType: String,
        @Json(name = "digest") val digest: String,
        @Json(name = "previousTransaction") val previousTransaction: String,
        @Json(name = "version") val version: String,
    )
}

data class SuiDryRunTransactionResponse(
    @Json(name = "effects")
    val effects: Effects,
    @Json(name = "input")
    val input: Input,
) {

    data class Effects(
        @Json(name = "gasUsed")
        val gasUsed: GasUsed,
    ) {

        data class GasUsed(
            @Json(name = "computationCost")
            val computationCost: BigDecimal,
            @Json(name = "nonRefundableStorageFee")
            val nonRefundableStorageFee: BigDecimal,
            @Json(name = "storageCost")
            val storageCost: BigDecimal,
            @Json(name = "storageRebate")
            val storageRebate: BigDecimal,
        )
    }

    data class Input(
        @Json(name = "gasData")
        val gasData: GasData,
    ) {

        data class GasData(
            @Json(name = "budget")
            val budget: BigDecimal,
            @Json(name = "owner")
            val owner: String,
            @Json(name = "payment")
            val payment: List<Payment>,
            @Json(name = "price")
            val price: BigDecimal,
        ) {

            data class Payment(
                @Json(name = "digest")
                val digest: String,
                @Json(name = "objectId")
                val objectId: String,
                @Json(name = "version")
                val version: Int,
            )
        }
    }
}

internal data class SuiExecuteTransactionBlockResponse(
    @Json(name = "digest")
    val digest: String,
)

internal data class SuiGetTransactionBlockResponse(
    @Json(name = "digest")
    val digest: String,
)