package com.tangem.blockchain.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.math.BigDecimal

internal object BigDecimalAdapter : JsonAdapter<BigDecimal>() {

    override fun fromJson(reader: JsonReader): BigDecimal? {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()

        return BigDecimal(reader.nextString())
    }

    override fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.toPlainString())
        }
    }
}
