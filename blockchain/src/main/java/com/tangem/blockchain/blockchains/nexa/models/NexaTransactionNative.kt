package com.tangem.blockchain.blockchains.nexa.models

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import org.bitcoinj.script.ScriptBuilder

data class NexaTransactionNative(
    val inputs: List<NexaTxInputNative>,
    val outputs: List<NexaTxOutput>,
    val lockTime: Long,
    val version: Int = 0, // always 0
)

fun NexaTransactionNative.prepareForSign(
    hashes: List<ByteArray>
) : NexaTransactionNative {
    require(inputs.size == hashes.size)
    require(inputs.all { it.hashToSign == NexaTxInputNative.SignHash.Empty })

    return copy(
        inputs = inputs.mapIndexed { index, inp ->
            inp.copy(
                hashToSign = NexaTxInputNative.SignHash.ReadyForSign(
                    hash = hashes[index]
                )
            )
        }
    )
}

fun NexaTransactionNative.sign(
    signatures: List<ByteArray>,
    publicKey : ByteArray
) : NexaTransactionNative {
    require(inputs.size == signatures.size)
    require(inputs.all { it.hashToSign is NexaTxInputNative.SignHash.ReadyForSign })

    return copy(
        inputs = inputs.mapIndexed { index, inp ->
            val signature = signatures[index]

            val script = when(inp.addressType) {
                NexaAddressType.P2PKH -> {
                    ScriptBuilder()
                        .data(signature + byteArrayOf(0x00)) //TODO Signature.NEXA_SIGN_ALL
                        .data(publicKey)
                        .build()
                }
                NexaAddressType.TEMPLATE -> {
                    ScriptBuilder()
                        .data(ScriptBuilder().data(publicKey).build().program)
                        .data(signature)
                        .build()
                }
                else -> error("Unsupported")
            }

            inp.copy(
                hashToSign = NexaTxInputNative.SignHash.Signed(
                    hash = script.program
                )
            )
        }
    )
}