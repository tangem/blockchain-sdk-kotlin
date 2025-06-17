package com.tangem.blockchain.blockchains.ethereum.ens

import com.tangem.blockchain.common.toBlockchainSdkError
import org.kethereum.keccakshortcut.keccak
import com.tangem.blockchain.extensions.Result
import java.net.IDN

@Suppress("MagicNumber")
internal class DefaultENSNameProcessor : ENSNameProcessor {

    override fun getNamehash(name: String): Result<ByteArray> = try {
        val normalized = normalizeAndCheckName(name)
        val labels = normalized.split(".")
        var node = ByteArray(32) { 0 }

        for (label in labels.reversed()) {
            val labelHash = label.toByteArray(Charsets.UTF_8).keccak()
            node = (node + labelHash).keccak()
        }

        Result.Success(node)
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    override fun encode(name: String): Result<ByteArray> = try {
        val normalized = normalizeAndCheckName(name)
        val labels = normalized.split(".")
        val encoded = ByteArray(labels.sumOf { it.length + 1 } + 1)
        var offset = 0

        for (label in labels) {
            require(label.length in 1..63) { "ENS label too short or too long: $label" }
            encoded[offset] = label.length.toByte()
            offset++
            label.toByteArray().copyInto(encoded, offset)
            offset += label.length
        }
        encoded[offset] = 0
        Result.Success(encoded)
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    private fun normalizeAndCheckName(name: String): String {
        val normalized = IDN.toASCII(name, IDN.USE_STD3_ASCII_RULES).lowercase()

        require(ENS_NAME_REGEX.matches(normalized)) {
            "Invalid ENS name after normalization: $normalized"
        }
        return normalized
    }

    private companion object {
        val ENS_NAME_REGEX = Regex("^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$")
    }
}