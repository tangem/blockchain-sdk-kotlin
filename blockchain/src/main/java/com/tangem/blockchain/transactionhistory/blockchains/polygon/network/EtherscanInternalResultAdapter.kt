package com.tangem.blockchain.transactionhistory.blockchains.polygon.network

import com.squareup.moshi.*

internal class EtherscanInternalResultAdapter {
    @FromJson
    fun fromJson(
        jsonReader: JsonReader,
        delegate: JsonAdapter<List<EtherscanInternalTransaction>>,
    ): EtherscanInternalResult {
        return if (jsonReader.peek() == JsonReader.Token.BEGIN_ARRAY) {
            EtherscanInternalResult.Transactions(delegate.fromJson(jsonReader).orEmpty())
        } else {
            EtherscanInternalResult.Description(jsonReader.nextString())
        }
    }

    @ToJson
    fun toJson(
        jsonWriter: JsonWriter,
        result: EtherscanInternalResult,
        delegate: JsonAdapter<List<EtherscanInternalTransaction>>,
    ) {
        when (result) {
            is EtherscanInternalResult.Description -> jsonWriter.value(result.description)
            is EtherscanInternalResult.Transactions -> delegate.toJson(jsonWriter, result.txs)
        }
    }
}