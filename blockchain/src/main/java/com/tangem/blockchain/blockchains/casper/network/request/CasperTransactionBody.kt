package com.tangem.blockchain.blockchains.casper.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CasperTransactionBody(
    @Json(name = "hash")
    val hash: String,

    @Json(name = "header")
    val header: Header,

    @Json(name = "payment")
    val payment: Payment,

    @Json(name = "session")
    val session: Session,

    @Json(name = "approvals")
    val approvals: List<Approval>,

) {
    @JsonClass(generateAdapter = true)
    data class Header(
        @Json(name = "account")
        val account: String,

        @Json(name = "timestamp")
        val timestamp: String,

        @Json(name = "ttl")
        val ttl: String,

        @Json(name = "gas_price")
        val gasPrice: Long,

        @Json(name = "body_hash")
        val bodyHash: String,

        @Json(name = "chain_name")
        val chainName: String,

        @Json(name = "dependencies")
        val dependencies: List<String> = listOf(),
    )

    @JsonClass(generateAdapter = true)
    data class Approval(
        @Json(name = "signer")
        val signer: String,

        @Json(name = "signature")
        val signature: String,
    )

    @JsonClass(generateAdapter = true)
    data class Session(
        @Json(name = "Transfer")
        val transfer: Transfer,
    ) {
        @JsonClass(generateAdapter = true)
        data class Transfer(
            @Json(name = "args")
            val args: List<Any>,
        )
    }

    @JsonClass(generateAdapter = true)
    data class Payment(
        @Json(name = "ModuleBytes")
        val moduleBytesObj: ModuleBytes,
    ) {
        @JsonClass(generateAdapter = true)
        data class ModuleBytes(
            @Json(name = "module_bytes")
            val moduleBytes: String,

            @Json(name = "args")
            val args: List<Any>,
        )
    }

    @JsonClass(generateAdapter = true)
    data class CLValue(
        @Json(name = "bytes")
        val bytes: String,

        @Json(name = "cl_type")
        val clType: CLType,

        @Json(name = "parsed")
        val parsed: Any?,
    )

    /**
     * Check [com.tangem.blockchain.blockchains.casper.network.CasperCLTypeAdapter]
     * to see what this type is serialized to
     */
    sealed class CLType {
        data object U64 : CLType()
        data object U512 : CLType()
        data object String : CLType()
        data object PublicKey : CLType()
        data class Option(val innerType: CLType) : CLType()
        data object Unknown : CLType()
    }
}