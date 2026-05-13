package com.tangem.blockchain.transactionhistory.blockchains.solana.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

internal class SolanaAccountKeyAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): SolanaAccountKey {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> SolanaAccountKey(
                pubkey = reader.nextString(),
                isSigner = null,
                isWritable = null,
                source = null,
            )
            JsonReader.Token.BEGIN_OBJECT -> {
                var pubkey: String? = null
                var isSigner: Boolean? = null
                var isWritable: Boolean? = null
                var source: String? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "pubkey" -> pubkey = reader.nextStringOrNull()
                        "signer" -> isSigner = reader.nextBooleanOrNull()
                        "writable" -> isWritable = reader.nextBooleanOrNull()
                        "source" -> source = reader.nextStringOrNull()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                val resolvedPubkey = pubkey?.takeUnless { it.isBlank() }
                    ?: throw JsonDataException("Solana account key object does not contain a valid pubkey")

                SolanaAccountKey(
                    pubkey = resolvedPubkey,
                    isSigner = isSigner,
                    isWritable = isWritable,
                    source = source,
                )
            }
            else -> {
                throw JsonDataException("Unsupported Solana account key JSON token: ${reader.peek()}")
            }
        }
    }

    @Suppress("UnusedParameter")
    @ToJson
    fun toJson(writer: JsonWriter, value: SolanaAccountKey) {
        throw UnsupportedOperationException("Serialization is not supported")
    }

    private fun JsonReader.nextStringOrNull(): String? {
        return if (peek() == JsonReader.Token.NULL) nextNull<String>() else nextString()
    }

    private fun JsonReader.nextBooleanOrNull(): Boolean? {
        return if (peek() == JsonReader.Token.NULL) nextNull<Boolean>() else nextBoolean()
    }
}