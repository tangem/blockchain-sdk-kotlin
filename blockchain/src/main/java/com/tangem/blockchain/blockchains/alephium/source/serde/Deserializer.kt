package com.tangem.blockchain.blockchains.alephium.source.serde

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isEmpty

@Suppress("FunctionNaming")
internal fun interface Deserializer<T> {

    fun _deserialize(input: ByteString): Result<Staging<T>>

    fun deserialize(input: ByteString): Result<T> {
        return _deserialize(input).map { (output, rest) ->
            if (rest.isEmpty()) {
                Result.success(output)
            } else {
                Result.failure(SerdeError.redundant(input.size - rest.size, input.size))
            }
        }.getOrElse { Result.failure(it) }
    }

    fun <U> validateGet(get: (T) -> U?, error: (T) -> String): Deserializer<U> {
        return Deserializer { input ->
            this._deserialize(input).map { (t, rest) ->
                val u = get(t)
                if (u != null) {
                    Result.success(Staging(u, rest))
                } else {
                    Result.failure(SerdeError.wrongFormat(error(t)))
                }
            }.getOrElse { Result.failure(it) }
        }
    }
}