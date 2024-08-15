package com.tangem.blockchain.blockchains.filecoin.network.response

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.adapter
import com.tangem.blockchain.network.moshi

/** Filecoin rpc response adapter [JsonAdapter] */
@OptIn(ExperimentalStdlibApi::class)
internal object FilecoinRpcResponseAdapter : JsonAdapter<FilecoinRpcResponse>() {

    private val options = JsonReader.Options.of("result", "error")

    private val failureAdapter by lazy { moshi.adapter<FilecoinRpcResponse.Failure>() }

    private val parsingResponseFailure = FilecoinRpcResponse.Failure("Parsing response failure")

    override fun fromJson(reader: JsonReader): FilecoinRpcResponse {
        var filecoinRpcResponse: FilecoinRpcResponse? = null

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> {
                    filecoinRpcResponse = runCatching { reader.readJsonValue() }
                        .mapCatching { FilecoinRpcResponse.Success(result = requireNotNull(it)) }
                        .getOrElse { parsingResponseFailure }
                }
                1 -> {
                    filecoinRpcResponse = runCatching { failureAdapter.fromJson(reader) }
                        .mapCatching { FilecoinRpcResponse.Failure(message = requireNotNull(it).message) }
                        .getOrElse { parsingResponseFailure }
                }
                -1 -> reader.skipNameAndValue()
            }
        }

        reader.endObject()

        return filecoinRpcResponse ?: FilecoinRpcResponse.Failure(message = "Unknown error")
    }

    override fun toJson(writer: JsonWriter, value: FilecoinRpcResponse?) {
        error("Not used")
    }

    private fun JsonReader.skipNameAndValue() {
        skipName()
        skipValue()
    }
}