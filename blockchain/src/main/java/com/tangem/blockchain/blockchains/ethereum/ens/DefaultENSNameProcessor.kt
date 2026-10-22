package com.tangem.blockchain.blockchains.ethereum.ens

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import org.kethereum.keccakshortcut.keccak
import com.tangem.blockchain.extensions.Result
import io.github.adraffy.ens.ENSNormalize

@Suppress("MagicNumber")
internal class DefaultENSNameProcessor : ENSNameProcessor {

    override fun getNamehash(name: String): Result<ByteArray> = catchingEnsErrors {
        val labels = normalizeAndCheckName(name).split(".")
        var node = ByteArray(32) { 0 }

        for (label in labels.reversed()) {
            val labelHash = label.toByteArray(Charsets.UTF_8).keccak()
            node = (node + labelHash).keccak()
        }

        node
    }

    override fun encode(name: String): Result<ByteArray> = catchingEnsErrors {
        val labels = normalizeAndCheckName(name).split(".").map { it.toByteArray(Charsets.UTF_8) }
        val encoded = ByteArray(labels.sumOf { it.size + 1 } + 1)
        var offset = 0

        for (label in labels) {
            require(label.size in 1..MAX_LABEL_BYTES) { "ENS label too short or too long: ${label.size} bytes" }
            encoded[offset] = label.size.toByte()
            offset++
            label.copyInto(encoded, offset)
            offset += label.size
        }
        encoded[offset] = 0
        encoded
    }

    private fun normalizeAndCheckName(name: String): String {
        val normalized = ENSNormalize.ENSIP15.normalize(name)

        require(normalized.split(".").size >= MIN_LABELS_COUNT) {
            "Invalid ENS name after normalization: $normalized"
        }
        return normalized
    }

    /**
     * Besides regular exceptions, catches [LinkageError]: ENSNormalize loads its Unicode tables from jar resources
     * in a static initializer, so missing resources surface as [ExceptionInInitializerError] / [NoClassDefFoundError].
     */
    private inline fun catchingEnsErrors(block: () -> ByteArray): Result<ByteArray> = try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    } catch (e: LinkageError) {
        Result.Failure(BlockchainSdkError.WrappedThrowable(e))
    }

    private companion object {
        const val MAX_LABEL_BYTES = 63
        const val MIN_LABELS_COUNT = 2
    }
}