package com.tangem.blockchain.blockchains.casper.network

import com.squareup.moshi.*
import com.tangem.blockchain.blockchains.casper.network.request.CasperTransactionBody

internal object CasperCLTypeAdapter : JsonAdapter<CasperTransactionBody.CLType>() {

    private const val CLTYPE_U64 = "U64"
    private const val CLTYPE_U512 = "U512"
    private const val CLTYPE_STRING = "String"
    private const val CLTYPE_PUBLIC_KEY = "PublicKey"
    private const val CLTYPE_OPTION = "Option"

    @FromJson
    override fun fromJson(jsonReader: JsonReader): CasperTransactionBody.CLType {
        return if (jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                val name: String = jsonReader.nextName()
                if (name == CLTYPE_OPTION) {
                    return CasperTransactionBody.CLType.Option(
                        parseCLType(jsonReader.nextString()),
                    )
                } else {
                    jsonReader.skipValue()
                }
            }
            jsonReader.endObject()

            CasperTransactionBody.CLType.Unknown
        } else {
            parseCLType(jsonReader.nextString())
        }
    }

    override fun toJson(jsonWriter: JsonWriter, value: CasperTransactionBody.CLType?) {
        when (value) {
            is CasperTransactionBody.CLType.Option ->
                jsonWriter
                    .beginObject()
                    .name(CLTYPE_OPTION)
                    .value(value.innerType.format())
                    .endObject()
            null -> return
            else -> jsonWriter.value(value.format())
        }
    }

    private fun CasperTransactionBody.CLType.format(): String = when (this) {
        is CasperTransactionBody.CLType.U64 -> CLTYPE_U64
        is CasperTransactionBody.CLType.U512 -> CLTYPE_U512
        is CasperTransactionBody.CLType.String -> CLTYPE_STRING
        is CasperTransactionBody.CLType.PublicKey -> CLTYPE_PUBLIC_KEY
        is CasperTransactionBody.CLType.Option -> error("`Option` doesn't have a string value")
        is CasperTransactionBody.CLType.Unknown -> error("`Unknown` is invalid value for CLType")
    }

    private fun parseCLType(value: String): CasperTransactionBody.CLType = when (value) {
        CLTYPE_U64 -> CasperTransactionBody.CLType.U64
        CLTYPE_U512 -> CasperTransactionBody.CLType.U512
        CLTYPE_STRING -> CasperTransactionBody.CLType.String
        CLTYPE_PUBLIC_KEY -> CasperTransactionBody.CLType.PublicKey
        else -> CasperTransactionBody.CLType.Unknown
    }
}