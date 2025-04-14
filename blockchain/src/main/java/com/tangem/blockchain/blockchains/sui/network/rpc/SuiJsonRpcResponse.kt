package com.tangem.blockchain.blockchains.sui.network.rpc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
internal data class SuiJsonRpcResponse<T : Any>(
    @Json(name = "jsonrpc") val jsonRpc: String,
    @Json(name = "id") val id: String,
    @Json(name = "result") val result: T?,
    @Json(name = "error") val error: SuiJsonRpcError?,
)

@JsonClass(generateAdapter = true)
internal data class SuiJsonRpcError(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
)

@JsonClass(generateAdapter = true)
internal data class SuiCoinsResponse(
    @Json(name = "data") val data: List<Data>,
    @Json(name = "hasNextPage") val hasNextPage: Boolean,
    @Json(name = "nextCursor") val nextCursor: String?,
) {

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "balance") val balance: BigDecimal,
        @Json(name = "coinObjectId") val coinObjectId: String,
        @Json(name = "coinType") val coinType: String,
        @Json(name = "digest") val digest: String,
        @Json(name = "previousTransaction") val previousTransaction: String,
        @Json(name = "version") val version: String,
    )
}

@JsonClass(generateAdapter = true)
data class SuiDryRunTransactionResponse(
    @Json(name = "effects")
    val effects: Effects,
    @Json(name = "input")
    val input: Input,
) {

    @JsonClass(generateAdapter = true)
    data class Effects(
        @Json(name = "gasUsed")
        val gasUsed: GasUsed,
        @Json(name = "status")
        val status: Status,
    ) {

        @JsonClass(generateAdapter = true)
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

        @JsonClass(generateAdapter = true)
        data class Status(
            @Json(name = "status")
            val value: String,
        ) {

            val isSuccess: Boolean
                get() = value == "success"
        }
    }

    @JsonClass(generateAdapter = true)
    data class Input(
        @Json(name = "gasData")
        val gasData: GasData,
    ) {

        @JsonClass(generateAdapter = true)
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

            @JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
internal data class SuiExecuteTransactionBlockResponse(
    @Json(name = "digest")
    val digest: String,
    @Json(name = "effects")
    val effects: Effects,
) {

    @JsonClass(generateAdapter = true)
    data class Effects(
        @Json(name = "status")
        val status: Status,
    ) {

        @JsonClass(generateAdapter = true)
        data class Status(
            @Json(name = "status")
            val value: String,
        ) {

            val isSuccess: Boolean
                get() = value == "success"
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class SuiGetTransactionBlockResponse(
    @Json(name = "digest")
    val digest: String,
)