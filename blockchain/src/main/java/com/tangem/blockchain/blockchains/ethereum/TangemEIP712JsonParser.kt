package com.tangem.blockchain.blockchains.ethereum

import com.tangem.common.extensions.toHexString
import org.kethereum.contract.abi.types.BYTES_COUNT_CONSTRAINT
import org.kethereum.contract.abi.types.INT_BITS_CONSTRAINT
import org.kethereum.contract.abi.types.extractPrefixedNumber
import org.kethereum.contract.abi.types.isETHType
import org.kethereum.contract.abi.types.isSupportedETHType
import org.kethereum.contract.abi.types.model.NamedETHType
import org.kethereum.contract.abi.types.model.type_params.BitsTypeParams
import org.kethereum.contract.abi.types.model.type_params.BytesTypeParams
import org.kethereum.contract.abi.types.model.types.AddressETHType
import org.kethereum.contract.abi.types.model.types.BoolETHType
import org.kethereum.contract.abi.types.model.types.BytesETHType
import org.kethereum.contract.abi.types.model.types.DynamicSizedBytesETHType
import org.kethereum.contract.abi.types.model.types.IntETHType
import org.kethereum.contract.abi.types.model.types.StringETHType
import org.kethereum.contract.abi.types.model.types.UIntETHType
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.model.Address
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.model.HexString
import pm.gnosis.eip712.DomainWithMessage
import pm.gnosis.eip712.EIP712JsonAdapter
import pm.gnosis.eip712.EIP712_DOMAIN_TYPE
import pm.gnosis.eip712.Literal712
import pm.gnosis.eip712.Struct712
import pm.gnosis.eip712.Struct712Parameter
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Custom JSON parser based on EIP712 standard.
 * This parser extends [pm.gnosis.eip712.EIP712JsonParser] to support objects array.
 *
 * @see pm.gnosis.eip712.EIP712JsonParser
[REDACTED_AUTHOR]
 */
internal class TangemEIP712JsonParser(private val jsonAdapter: EIP712JsonAdapter) {
    fun parseMessage(rawJson: String): DomainWithMessage {
        val adapterResult = jsonAdapter.parse(rawJson)
        return DomainWithMessage(
            domain = buildStruct712(
                typeName = EIP712_DOMAIN_TYPE,
                values = adapterResult.domain,
                typeSpec = adapterResult.types
            ),
            message = buildStruct712(
                typeName = adapterResult.primaryType,
                values = adapterResult.message,
                typeSpec = adapterResult.types
            )
        )
    }

    private fun buildStruct712(
        typeName: String,
        values: Map<String, Any>,
        typeSpec: Map<String, List<EIP712JsonAdapter.Parameter>>,
    ): Struct712 {
        val params = typeSpec[typeName] ?: throw IllegalArgumentException("TypedDate does not contain type $typeName")
        val innerParams = params.map { typeParam ->
            val type712 = if (typeSpec.contains(typeParam.type) || typeSpec.any { typeParam.type.startsWith(it.key) }) {
                if (typeParam.type.endsWith("[]")) {
                    // Array
                    val type = typeParam.type.replace(oldValue = "[]", newValue = "")
                    val innerParams = (values[typeParam.name] as List<Any>)
                        .map {
                            buildStruct712(typeName = type, values = it as Map<String, Any>, typeSpec = typeSpec)
                        }
                        .map { Struct712Parameter(name = type, type = it) }

                    Struct712(typeName = typeParam.name, parameters = innerParams)
                } else {
                    // Struct
                    buildStruct712(
                        typeName = typeParam.type,
                        values = values[typeParam.name] as Map<String, Any>,
                        typeSpec = typeSpec
                    )
                }
            } else {
                // Literal
                val rawValue = values[typeParam.name] ?: throw IllegalArgumentException(
                    "Could not get value for property ${typeParam.name}"
                )

                if (!NamedETHType(typeParam.type).isETHType()) {
                    throw IllegalArgumentException(
                        "Property with name ${typeParam.name} has invalid Solidity type ${typeParam.type}"
                    )
                }

                if (!NamedETHType(typeParam.type).isSupportedETHType()) {
                    throw IllegalArgumentException(
                        "Property with name ${typeParam.name} has unsupported Solidity type ${typeParam.type}"
                    )
                }

                val ethereumType = when {
                    typeParam.type.startsWith(prefix = "uint") -> {
                        readNumber(
                            rawNumber = rawValue,
                            creator = {
                                UIntETHType.ofNativeKotlinType(
                                    input = it,
                                    params = BitsTypeParams(
                                        bits = typeParam.type.extractPrefixedNumber("uint", INT_BITS_CONSTRAINT)
                                    )
                                )
                            }
                        )
                    }

                    typeParam.type.startsWith(prefix = "int") -> {
                        readNumber(
                            rawNumber = rawValue,
                            creator = {
                                IntETHType.ofNativeKotlinType(
                                    input = it,
                                    params = BitsTypeParams(
                                        typeParam.type.extractPrefixedNumber("int", INT_BITS_CONSTRAINT)
                                    )
                                )
                            }
                        )
                    }

                    typeParam.type == "bytes" -> {
                        DynamicSizedBytesETHType.ofNativeKotlinType(
                            HexString(string = rawValue as String).hexToByteArray()
                        )
                    }

                    typeParam.type == "string" -> StringETHType.ofString(rawValue.toString())
                    typeParam.type.startsWith(prefix = "bytes") -> {
                        BytesETHType.ofNativeKotlinType(
                            input = HexString(rawValue as String).hexToByteArray(),
                            params = BytesTypeParams(
                                typeParam.type.extractPrefixedNumber("bytes", BYTES_COUNT_CONSTRAINT)
                            )
                        )
                    }

                    typeParam.type == "bool" -> readBool(rawBool = rawValue)
                    typeParam.type == "address" -> {
                        readNumber(
                            rawNumber = rawValue,
                            creator = {
                                AddressETHType.ofNativeKotlinType(Address(it.toByteArray().toHexString()))
                            }
                        )
                    }

                    else -> throw IllegalArgumentException("Unknown literal type ${typeParam.type}")
                }

                Literal712(typeName = typeParam.type, value = ethereumType)
            }

            Struct712Parameter(name = typeParam.name, type = type712)
        }

        return Struct712(typeName = typeName, parameters = innerParams)
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

    private fun readBool(rawBool: Any): BoolETHType {
        return if (rawBool is Boolean) {
            BoolETHType.ofNativeKotlinType(rawBool)
        } else if (rawBool.toString().equals("true", ignoreCase = true) ||
            rawBool.toString().equals("false", ignoreCase = true)
        ) {
            BoolETHType.ofNativeKotlinType(rawBool.toString().equals("true", ignoreCase = true))
        } else {
            throw java.lang.IllegalArgumentException("Value $rawBool is not a Boolean")
        }
    }

    private fun BigDecimal.exactNumber(): BigInteger {
        return try {
            toBigIntegerExact()
        } catch (e: Exception) {
            throw IllegalArgumentException("Value ${toString()} is a decimal (not supported)")
        }
    }
}