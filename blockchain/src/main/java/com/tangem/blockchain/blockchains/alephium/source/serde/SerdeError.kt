package com.tangem.blockchain.blockchains.alephium.source.serde

internal sealed class SerdeError(override val message: String) : Exception(message) {

    class NotEnoughBytes(message: String) : SerdeError(message)
    class WrongFormat(message: String) : SerdeError(message)
    class Validation(message: String) : SerdeError(message)
    class Other(message: String) : SerdeError(message)

    companion object {
        // this is returned when deserializing with partial bytes
        fun notEnoughBytes(expected: Int, got: Int): NotEnoughBytes =
            NotEnoughBytes("Too few bytes: expected $expected, got $got")

        fun incompleteData(expected: Int, got: Int): WrongFormat =
            WrongFormat("Too few bytes: expected $expected, got $got")

        fun redundant(expected: Int, got: Int): WrongFormat =
            WrongFormat("Too many bytes: expected $expected, got $got")

        fun validation(message: String): Validation = Validation(message)

        fun wrongFormat(message: String): WrongFormat = WrongFormat(message)

        fun other(message: String): Other = Other(message)
    }
}