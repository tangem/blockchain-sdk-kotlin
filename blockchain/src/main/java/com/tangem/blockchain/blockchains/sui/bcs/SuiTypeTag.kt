package com.tangem.blockchain.blockchains.sui.bcs

/**
 * Move `TypeTag`. `TypeInput`, which programmable move calls take as type arguments, shares its BCS layout.
 */
internal sealed interface SuiTypeTag {

    fun write(writer: SuiBcsWriter)

    data class Primitive(val tag: Int) : SuiTypeTag {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(tag)
        }
    }

    data class Vector(val element: SuiTypeTag) : SuiTypeTag {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_VECTOR)
            element.write(writer)
        }
    }

    data class Struct(
        val address: String,
        val module: String,
        val name: String,
        val typeParams: List<SuiTypeTag>,
    ) : SuiTypeTag {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_STRUCT)
                .address(address)
                .string(module)
                .string(name)
                .vector(typeParams) { it.write(this) }
        }
    }

    companion object {
        private const val TAG_VECTOR = 6
        private const val TAG_STRUCT = 7

        private val PRIMITIVES = mapOf(
            "bool" to 0,
            "u8" to 1,
            "u64" to 2,
            "u128" to 3,
            "address" to 4,
            "signer" to 5,
            "u16" to 8,
            "u32" to 9,
            "u256" to 10,
        )

        /** Parses a type string such as `0x2::sui::SUI` or `0xabc::pool::LP<0x2::sui::SUI, u64>` */
        fun parse(type: String): SuiTypeTag {
            val parser = Parser(type.filterNot(Char::isWhitespace))
            val tag = parser.parseType()
            require(parser.isAtEnd) { "Unexpected trailing characters in Sui type: $type" }
            return tag
        }
    }

    private class Parser(private val source: String) {

        private var position = 0

        val isAtEnd: Boolean get() = position == source.length

        fun parseType(): SuiTypeTag {
            val head = readUntilAny(chars = "<>,")
            PRIMITIVES[head]?.let { return Primitive(it) }

            if (head == "vector") {
                expect('<')
                val element = parseType()
                expect('>')
                return Vector(element)
            }

            val parts = head.split("::")
            require(parts.size == STRUCT_PARTS_COUNT) { "Invalid Sui struct type: $head" }
            val typeParams = mutableListOf<SuiTypeTag>()
            if (peek() == '<') {
                expect('<')
                do {
                    typeParams.add(parseType())
                } while (consumeIf(','))
                expect('>')
            }
            return Struct(address = parts[0], module = parts[1], name = parts[2], typeParams = typeParams)
        }

        private fun readUntilAny(chars: String): String {
            val start = position
            while (position < source.length && source[position] !in chars) position++
            return source.substring(start, position)
        }

        private fun peek(): Char? = source.getOrNull(position)

        private fun consumeIf(char: Char): Boolean = (peek() == char).also { if (it) position++ }

        private fun expect(char: Char) {
            require(consumeIf(char)) { "Expected '$char' at $position in Sui type: $source" }
        }

        private companion object {
            const val STRUCT_PARTS_COUNT = 3
        }
    }
}