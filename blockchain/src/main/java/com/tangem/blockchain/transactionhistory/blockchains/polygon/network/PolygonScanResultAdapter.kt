package com.tangem.blockchain.transactionhistory.blockchains.polygon.network

import com.squareup.moshi.*

internal class PolygonScanResultAdapter {
    @FromJson
    fun fromJson(jsonReader: JsonReader, delegate: JsonAdapter<List<PolygonTransaction>>): PolygonScanResult {
        return if (jsonReader.peek() == JsonReader.Token.BEGIN_ARRAY) {
            PolygonScanResult.Transactions(delegate.fromJson(jsonReader).orEmpty())
        } else {
            PolygonScanResult.Description(jsonReader.nextString())
        }
    }

    @ToJson
    fun toJson(
        jsonWriter: JsonWriter,
        polygonScanResult: PolygonScanResult,
        delegate: JsonAdapter<PolygonScanResult.Transactions>,
    ) {
        when (polygonScanResult) {
            is PolygonScanResult.Description -> jsonWriter.value(polygonScanResult.description)
            is PolygonScanResult.Transactions -> jsonWriter.value(delegate.toJson(polygonScanResult))
        }
    }
}