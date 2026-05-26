package com.tangem.blockchain.transactionhistory.blockchains.solana.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

internal class SolanaInstructionAdapter {

    @FromJson
    fun fromJson(reader: JsonReader, parsedAdapter: JsonAdapter<SolanaParsedInstruction>): SolanaInstruction {
        var programId: String? = null
        var program: String? = null
        var parsed: SolanaParsedInstruction? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "programId" -> programId = reader.nextStringOrNull()
                "program" -> program = reader.nextStringOrNull()
                "parsed" -> {
                    parsed = if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        parsedAdapter.fromJson(reader)
                    } else {
                        reader.skipValue()
                        null
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return SolanaInstruction(programId = programId, program = program, parsed = parsed)
    }

    @Suppress("UnusedParameter")
    @ToJson
    fun toJson(writer: JsonWriter, value: SolanaInstruction) {
        // Not needed — read-only model
        throw UnsupportedOperationException("Serialization is not supported")
    }

    private fun JsonReader.nextStringOrNull(): String? {
        return if (peek() == JsonReader.Token.NULL) {
            nextNull<String>()
        } else {
            nextString()
        }
    }
}