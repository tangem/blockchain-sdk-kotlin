package com.tangem.blockchain.blockchains.ethereum.eip712

import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.serializersModuleOf
import org.kethereum.contract.abi.types.*
import org.kethereum.contract.abi.types.model.ETHType
import org.kethereum.contract.abi.types.model.type_params.BitsTypeParams
import org.kethereum.contract.abi.types.model.type_params.BytesTypeParams
import org.kethereum.contract.abi.types.model.types.*
import org.kethereum.crypto.signMessageHash
import org.kethereum.crypto.toECKeyPair
import org.kethereum.crypto.toHex
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.PrivateKey
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.model.HexString
import java.math.BigDecimal
import java.math.BigInteger

/**
 * It is a fork of https://github.com/blocto/ethereum-sign-util.kotlin library,
 * with necessary fixes to make it work with various types of eip-712 data from different dApps.
 */

/**
 * Represents the version of `signTypedData` being used.
 *
 * V1 is based upon [an early version of EIP-712](https://github.com/ethereum/EIPs/pull/712/commits/21abe254fe0452d8583d5b132b1d7be87c0439ca)
 * that lacked some later security improvements, and should generally be neglected in favor of
 * later versions.
 *
 * V3 is based on EIP-712, except that arrays and recursive data structures are not supported.
 *
 * V4 is based on EIP-712, and includes full support of arrays and recursive data structures.
 */
private enum class SignTypedDataVersion {
    V1,
    V3,
    V4,
}

private typealias TypedDataV1 = String

@Serializable
private data class MessageTypeProperty(val name: String, val type: String)

@Serializable
private data class TypedMessage(
    val types: Map<String, List<MessageTypeProperty>>,
    val primaryType: String,
    val domain: Map<String, @Contextual Any>,
    val message: Map<String, @Contextual Any>,
)

@Serializable
private data class TypedDataV1Field(
    val name: String,
    val type: String,
    val value: @Contextual Any,
)

object AnySerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    override fun serialize(encoder: Encoder, value: Any) = Unit

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return deserializeJsonElement(element)
    }

    private fun deserializeJsonElement(element: JsonElement): Any = when (element) {
        is JsonObject -> element.mapValues { deserializeJsonElement(it.value) }
        is JsonArray -> element.map { deserializeJsonElement(it) }
        is JsonPrimitive -> if (element.isString) element.content else element.toString()
    }
}

internal object EthEip712Util {
    private const val EIP712_DOMAIN_TYPE = "EIP712Domain"
    private val json = Json { serializersModule = serializersModuleOf(AnySerializer) }

    /**
     * Create an Ethereum-specific signature for a message.
     *
     * This function is equivalent to the `eth_sign` Ethereum JSON-RPC method as specified in EIP-1417,
     * as well as the MetaMask's `personal_sign` method.
     *
     * @param options.privateKey - The key to sign with.
     * @param options.data - The hex data to sign.
     * @returns The '0x'-prefixed hex encoded signature.
     */
    fun personalSign(
        privateKey: ByteArray,
        data: Any,
    ): String {
        val message = legacyToBuffer(data)
        val msgHash = hashPersonalMessage(message)
        val ecKeyPair = PrivateKey(privateKey).toECKeyPair()
        val sig = signMessageHash(msgHash, ecKeyPair)
        return "0x${sig.toHex()}"
    }

    /**
     * Returns the keccak-256 hash of `message`, prefixed with the header used by the `eth_sign` RPC call.
     * The output of this function can be fed into `ecsign` to produce the same signature as the `eth_sign`
     * call for a given `message`, or fed to `ecrecover` along with a signature to recover the public key
     * used to produce the signature.
     */
    private fun hashPersonalMessage(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()
        return (prefix + message).keccak()
    }

    /**
     * Validate that the given value is a valid version string.
     *
     * @param version - The version value to validate.
     * @param allowedVersions - A list of allowed versions. If omitted, all versions are assumed to be
     * allowed.
     */
    private fun validateVersion(
        version: SignTypedDataVersion,
        allowedVersions: List<SignTypedDataVersion>?,
    ) {
        if (!SignTypedDataVersion.values().contains(version)) {
            throw Error("Invalid version: '$version'")
        } else if (allowedVersions != null && !allowedVersions.contains(version)) {
            throw Error(
                "SignTypedDataVersion not allowed: '$version'. " +
                    "Allowed versions are: ${allowedVersions.joinToString(", ")}"
            )
        }
    }

    private fun soliditySHA3(types: List<String>, values: List<Any>): ByteArray {
        return (solidityPack(types, values)).keccak()
    }

    private fun solidityPack(types: List<String>, values: List<Any>): ByteArray {
        if (types.size != values.size) {
            throw Error("Number of types are not matching the values")
        }

        var ret = byteArrayOf()

        for (i in types.indices) {
            val type = elementaryName(types[i])
            val value = values[i]
            ret += solidityHexValue(type, value)
        }

        return ret
    }

    private fun solidityHexValue(type: String, value: Any): ByteArray = when {
        type.lastIndexOf("]") == type.length - 1 -> {
            val subType = type.substringBefore("[")
            var arrayValues = byteArrayOf()
            (value as Iterable<*>).filterNotNull().map { arrayValues += solidityHexValue(subType, it) }
            BytesETHType(arrayValues, BytesTypeParams(32)).paddedValue
        }

        type == "bytes" -> BytesETHType((value as ByteArray), BytesTypeParams(32)).paddedValue
        type == "string" -> BytesETHType(
            value.toString().toByteArray(),
            BytesTypeParams(32)
        ).paddedValue

        type == "bool" -> readBool(rawBool = value).paddedValue
        type == "address" -> readNumber(rawNumber = value,
            creator = {
                AddressETHType.ofNativeKotlinType(Address(it.toByteArray().toHexString()))
            }).toKotlinType().hex.let { HexString(it).hexToByteArray() }

        type.startsWith("bytes") -> BytesETHType(
            (value as ByteArray),
            BytesTypeParams(32)
        ).toKotlinType()

        type.startsWith("uint") -> readNumber(rawNumber = value,
            creator = {
                UIntETHType.ofNativeKotlinType(
                    it,
                    BitsTypeParams(
                        type.extractPrefixedNumber(
                            "uint",
                            INT_BITS_CONSTRAINT
                        )
                    )
                )
            }).paddedValue

        type.startsWith("int") -> readNumber(rawNumber = value,
            creator = {
                IntETHType.ofNativeKotlinType(
                    it,
                    BitsTypeParams(
                        type.extractPrefixedNumber(
                            "int",
                            INT_BITS_CONSTRAINT
                        )
                    )
                )
            }).toKotlinType().toByteArray()

        else -> throw Error("Unsupported or invalid type: $type")
    }

    private fun elementaryName(name: String): String = when {
        name.startsWith("int[") -> "int256" + name.drop(3)
        name === "int" -> "int256"
        name.startsWith("uint[") -> "uint256" + name.drop(4)
        name === "uint" -> "uint256"
        name.startsWith("fixed[") -> "fixed128x128" + name.drop(5)
        name === "fixed" -> "fixed128x128"
        name.startsWith("ufixed[") -> "ufixed128x128" + name.drop(6)
        name === "ufixed" -> "ufixed128x128"
        else -> name
    }

    /**
     * Generate the "V1" hash for the provided typed message.
     *
     * The hash will be generated in accordance with an earlier version of the EIP-712
     * specification. This hash is used in `signTypedData_v1`.
     *
     * @param typedData - The typed message.
     * @returns The '0x'-prefixed hex encoded hash representing the type of the provided message.
     */
    fun typedSignatureHash(typedData: TypedDataV1): String =
        _typedSignatureHash(json.decodeFromString(typedData)).toHexString()

    /**
     * Generate the "V1" hash for the provided typed message.
     *
     * The hash will be generated in accordance with an earlier version of the EIP-712
     * specification. This hash is used in `signTypedData_v1`.
     *
     * @param typedData - The typed message.
     * @returns The hash representing the type of the provided message.
     */
    private fun _typedSignatureHash(typedData: List<TypedDataV1Field>): ByteArray {
        val data = typedData.map {
            if (it.type != "bytes") {
                it.value
            } else {
                legacyToBuffer(it.value)
            }
        }
        val types = typedData.map { it.type }
        val schema = typedData.map { "${it.type} ${it.name}" }
        return soliditySHA3(
            listOf("bytes32", "bytes32"),
            listOf(
                soliditySHA3(List(typedData.size) { "string" }, schema),
                soliditySHA3(types, data),
            )
        )
    }

    /**
     * Removes properties from a message object that are not defined per EIP-712.
     *
     * @param data - The typed message object.
     * @returns The typed message object with only allowed fields.
     */
    private fun sanitizeData(data: String): TypedMessage = json.decodeFromString(data)

    @Suppress("UNCHECKED_CAST")
    private fun encodeField(
        types: Map<String, List<MessageTypeProperty>>,
        type: String,
        value: Any,
    ): ETHType<out Any> {
        when {
            !types[type].isNullOrEmpty() -> {
                val data = encodeData(types, type, value as Map<String, Any>)
                return BytesETHType(data, BytesTypeParams(32))
            }

            type == "bytes" -> {
                return BytesETHType((value as String).hexToBytes().keccak(), BytesTypeParams(32))
            }

            type == "string" -> {
                // convert string to buffer - prevents ethUtil from interpreting strings like '0xabcd' as hex
                if (value is String) {
                    return BytesETHType(value.toByteArray().keccak(), BytesTypeParams(32))
                }
            }
        }

        if (type.lastIndexOf("]") == type.length - 1) {
            val parsedType = type.substringBefore("[")
            val typeValuePairs = (value as Iterable<*>).filterNotNull().map { item ->
                encodeField(types, parsedType, item)
            }
            return BytesETHType(
                encodeTypes(*typeValuePairs.toTypedArray()).keccak(),
                BytesTypeParams(32)
            )
        }

        return when {
            type.startsWith(prefix = "uint") -> readNumber(rawNumber = value,
                creator = {
                    UIntETHType.ofNativeKotlinType(
                        it,
                        BitsTypeParams(
                            type.extractPrefixedNumber(
                                "uint",
                                INT_BITS_CONSTRAINT
                            )
                        )
                    )
                })

            type.startsWith(prefix = "int") -> readNumber(rawNumber = value,
                creator = {
                    IntETHType.ofNativeKotlinType(
                        it,
                        BitsTypeParams(
                            type.extractPrefixedNumber(
                                "int",
                                INT_BITS_CONSTRAINT
                            )
                        )
                    )
                })

            type == "bytes" -> DynamicSizedBytesETHType.ofNativeKotlinType(HexString(value as String).hexToByteArray())
            type == "string" -> BytesETHType(
                (value as String).toByteArray().keccak(),
                BytesTypeParams(32)
            )

            type.startsWith(prefix = "bytes") -> BytesETHType.ofNativeKotlinType(
                (value as String).hexToBytes(),
                BytesTypeParams(
                    (type.extractPrefixedNumber(
                        "bytes",
                        BYTES_COUNT_CONSTRAINT
                    ))
                )
            )

            type == "bool" -> readBool(rawBool = value)
            type == "address" -> readNumber(rawNumber = value,
                creator = {
                    AddressETHType.ofNativeKotlinType(Address(it.toByteArray().toHexString()))
                })

            else -> throw IllegalArgumentException("Unknown literal type ${type}")
        }
    }

    private fun encodeData(
        typeSpec: Map<String, List<MessageTypeProperty>>,
        typeName: String,
        values: Map<String, Any>,
    ): ByteArray {
        var encodedTypes = byteArrayOf()
        val encodedValues = mutableListOf<ETHType<out Any>>()

        val types = typeHash(typeSpec, typeName).joinToString(separator = "")
        val typeHash = types.toByteArray(charset = Charsets.UTF_8).keccak()
        encodedTypes += typeHash

        for (field in typeSpec[typeName]!!) {
            val hash = encodeField(typeSpec, field.type, values[field.name]!!)
            encodedValues.add(hash)
        }
        return (encodedTypes + encodeTypes(*encodedValues.toTypedArray())).keccak()
    }

    private fun typeHash(
        typeSpec: Map<String, List<MessageTypeProperty>>,
        typeName: String,
    ): List<String> {
        val types = typeSpec[typeName] ?: emptyList()
        val encodedStruct = types
            .joinToString(separator = ",",
                prefix = "$typeName(",
                postfix = ")",
                transform = { (name, type) -> "$type $name" })
        val structParams = types.asSequence()
            .filterNot { typeSpec[it.type.substringBefore("[")] == null }
            .map { (_, type) -> typeHash(typeSpec, type.substringBefore("[")) }
            .flatten().distinct().sorted().toList()
        return listOf(encodedStruct) + structParams
    }

    private fun <T> readNumber(rawNumber: Any, creator: (BigInteger) -> T): T =
        when (rawNumber) {
            is Number -> creator(BigDecimal(rawNumber.toString()).exactNumber())
            is String -> {
                if (rawNumber.startsWith(prefix = "0x")) creator(HexString(rawNumber).hexToBigInteger())
                else creator(BigDecimal(rawNumber).exactNumber())
            }

            else -> throw IllegalArgumentException("Value $rawNumber is neither a Number nor String")
        }

    private fun readBool(rawBool: Any): BoolETHType =
        if (rawBool is Boolean) BoolETHType.ofNativeKotlinType(rawBool)
        else if (rawBool.toString().equals("true", ignoreCase = true) || rawBool.toString()
                .equals("false", ignoreCase = true)
        )
            BoolETHType.ofNativeKotlinType(rawBool.toString().equals("true", ignoreCase = true))
        else throw java.lang.IllegalArgumentException("Value $rawBool is not a Boolean")

    private fun BigDecimal.exactNumber() =
        try {
            toBigIntegerExact()
        } catch (e: Exception) {
            throw IllegalArgumentException("Value ${toString()} is a decimal (not supported)")
        }

    /**
     * Hash a typed message according to EIP-712. The returned message starts with the EIP-712 prefix,
     * which is "1901", followed by the hash of the domain separator, then the data (if any).
     * The result is hashed again and returned.
     *
     * This function does not sign the message. The resulting hash must still be signed to create an
     * EIP-712 signature.
     *
     * @param typedData - The typed message to hash.
     * @returns The hash of the typed message.
     */
    fun eip712Hash(
        typedData: String,
    ): ByteArray {
        val sanitizedData = sanitizeData(typedData)
        var parts = byteArrayOf(0x19, 0x1)
        parts += hashStruct(
            encodeData(
                typeName = EIP712_DOMAIN_TYPE,
                typeSpec = sanitizedData.types,
                values = sanitizedData.domain
            )
        )
        if (sanitizedData.primaryType != EIP712_DOMAIN_TYPE) {
            parts += hashStruct(
                encodeData(
                    typeName = sanitizedData.primaryType,
                    typeSpec = sanitizedData.types,
                    values = sanitizedData.message
                )
            )
        }
        return parts.keccak()
    }

    private fun hashStruct(encodeData: ByteArray): ByteArray = encodeData

    /**
     * Convert a value to a Buffer. This function should be equivalent to the `toBuffer` function in
     * `ethereumjs-util@5.2.1`.
     *
     * @param value - The value to convert to a Buffer.
     * @returns The given value as a Buffer.
     */
    private fun legacyToBuffer(value: Any): ByteArray {
        return when (value is String && !isHexString(value)) {
            true -> value.toByteArray()
            false -> toBuffer(value)
        }
    }

    /**
     * Is the string a hex string.
     *
     * @method check if string is hex string of specific length
     * @param {String} value
     * @param {Number} length
     * @returns {Boolean} output the string is a hex string
     */
    private fun isHexString(value: Any, length: Int? = null): Boolean {
        return when {
            value !is String || !value.matches(Regex("^0x[0-9A-Fa-f]*")) -> false
            length != null && value.length != 2 + 2 * length -> false
            else -> true
        }
    }

    /**
     * Attempts to turn a value into a `Buffer`.
     * Inputs supported: `Buffer`, `String`, `Number`, null/undefined, `BN` and other objects with a `toArray()` or `toBuffer()` method.
     * @param v the value
     */
    private fun toBuffer(value: Any): ByteArray {
        return when (value) {
            is ByteArray -> value.clone()
            is Array<*> -> iterableToByteArray(value.toList())
            is Iterable<*> -> iterableToByteArray(value)
            is String -> {
                if (!isHexString(value)) {
                    throw Error("Cannot convert string to buffer. toBuffer only supports hex strings and this string was given: $value")
                }
                HexString(padToEven(stripHexPrefix(value))).hexToByteArray()
            }

            is Int -> intToBuffer(value)
            is BigInteger -> value.toByteArray()
            else -> throw Error("invalid type")
        }
    }

    private fun iterableToByteArray(value: Iterable<*>): ByteArray {
        var out = byteArrayOf()
        value.map {
            out += when (it) {
                is Number -> byteArrayOf(it.toByte())
                else -> byteArrayOf(0x0)
            }
        }
        return out
    }

    /**
     * Pads a `String` to have an even length
     * @param {String} value
     * @return {String} output
     */
    private fun padToEven(string: String): String = when {
        string.length % 2 == 1 -> "0${string}"
        else -> string
    }

    private fun isEven(n: Int): Boolean = n and 1 == 0

    /**
     * Removes '0x' from a given `String` if present
     * @param {String} str the string value
     * @return {String|Optional} a string by pass if necessary
     */
    private fun stripHexPrefix(str: String): String =
        if (isHexPrefixed(str)) str.drop(2) else str

    private fun isHexPrefixed(str: Any): Boolean {
        if (str !is String) {
            throw Error("[is-hex-prefixed] value must be type 'string', is currently type ${str}, while checking isHexPrefixed.")
        }
        return str.slice(0..2) == "0x"
    }

    /**
     * Converts an `Number` to a `Buffer`
     * @param {Number} i
     * @return {Buffer}
     */
    private fun intToBuffer(i: Int): ByteArray {
        val hex = intToHex(i)
        return HexString(padToEven(hex.drop(2))).hexToByteArray()
    }

    /**
     * Converts a `Number` into a hex `String`
     * @param {Number} i
     * @return {String}
     */
    private fun intToHex(i: Int): String {
        val hex = Integer.toHexString(i)
        return "0x$hex"
    }
}