package com.tangem.blockchain.blockchains.casper.network.response

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.adapter
import com.tangem.blockchain.network.moshi

/** Casper rpc response adapter [JsonAdapter] */
@OptIn(ExperimentalStdlibApi::class)
internal object CasperRpcResponseAdapter : JsonAdapter<CasperRpcResponse>() {

    private val options = JsonReader.Options.of("result", "error")

    private val failureAdapter by lazy { moshi.adapter<CasperRpcResponse.Failure>() }

    private val parsingResponseFailure = CasperRpcResponse.Failure("Parsing response failure")

    override fun fromJson(reader: JsonReader): CasperRpcResponse {
        var casperRpcResponse: CasperRpcResponse? = null

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> {
                    casperRpcResponse = runCatching { reader.readJsonValue() }
                        .mapCatching { CasperRpcResponse.Success(result = requireNotNull(it)) }
                        .getOrElse { parsingResponseFailure }
                }
                1 -> {
                    casperRpcResponse = runCatching { failureAdapter.fromJson(reader) }
                        .mapCatching { CasperRpcResponse.Failure(message = requireNotNull(it).message) }
                        .getOrElse { parsingResponseFailure }
                }
                -1 -> reader.skipNameAndValue()
            }
        }

        reader.endObject()

        return casperRpcResponse ?: CasperRpcResponse.Failure(message = "Unknown error")
    }

    override fun toJson(writer: JsonWriter, value: CasperRpcResponse?) {
        error("Not used")
    }

    private fun JsonReader.skipNameAndValue() {
        skipName()
        skipValue()
    }
}