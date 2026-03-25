package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptOpCodes
import org.junit.Test

/**
 * Unit tests for BitcoinMemoBuilder.
 *
 * Tests cover:
 * - Successful memo output creation
 * - Hex string parsing and validation
 * - Size limit enforcement (80 bytes max)
 * - OP_RETURN script structure
 */
class BitcoinMemoBuilderTest {

    private val networkParams = MainNetParams()

    @Test
    fun `addMemoOutput creates valid OP_RETURN output with short memo`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "48656c6c6f20576f726c64" // "Hello World" in ASCII

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(transaction.outputs).hasSize(1)

        val output = transaction.outputs[0]
        assertThat(output.value.value).isEqualTo(0L) // Zero satoshis
        assertThat(output.scriptPubKey.chunks[0].opcode).isEqualTo(ScriptOpCodes.OP_RETURN)
    }

    @Test
    fun `addMemoOutput handles maximum size memo (80 bytes)`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "A".repeat(160) // 80 bytes (160 hex chars)

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(transaction.outputs).hasSize(1)
    }

    @Test
    fun `addMemoOutput rejects memo exceeding 80 bytes`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "A".repeat(162) // 81 bytes (162 hex chars)

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        val failure = result as Result.Failure
        assertThat(failure.error).isInstanceOf(BlockchainSdkError.CustomError::class.java)
        assertThat(failure.error.customMessage).contains("exceeds maximum size")
    }

    @Test
    fun `addMemoOutput handles empty memo`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = ""

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(transaction.outputs).hasSize(1)
    }

    @Test
    fun `addMemoOutput rejects invalid hex string (odd length)`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "48656c6c6f" + "A" // Odd length hex string

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        val failure = result as Result.Failure
        assertThat(failure.error.customMessage).contains("Invalid memo hex string")
    }

    @Test
    fun `addMemoOutput rejects invalid hex characters`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "48656c6c6fZZ" // Invalid hex chars

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        val failure = result as Result.Failure
        assertThat(failure.error.customMessage).contains("Invalid memo hex string")
    }

    @Test
    fun `addMemoOutput strips 0x prefix if present`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "0x48656c6c6f" // "Hello" with 0x prefix

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(transaction.outputs).hasSize(1)
    }

    @Test
    fun `addMemoOutput handles mixed case hex`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "48656C6C6F" // Mixed case

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat(transaction.outputs).hasSize(1)
    }

    @Test
    fun `estimateOpReturnOutputSize returns correct size for small memo`() {
        // Given
        val memoSizeBytes = 11 // "Hello World"

        // When
        val estimatedSize = BitcoinMemoBuilder.estimateOpReturnOutputSize(memoSizeBytes)

        // Then
        // Expected: 8 (value) + 1 (script length varint) + 1 (OP_RETURN) + 1 (push opcode) + 11 (data) = 22
        assertThat(estimatedSize).isEqualTo(22)
    }

    @Test
    fun `estimateOpReturnOutputSize returns correct size for 80 byte memo`() {
        // Given
        val memoSizeBytes = 80

        // When
        val estimatedSize = BitcoinMemoBuilder.estimateOpReturnOutputSize(memoSizeBytes)

        // Then
        // Expected: 8 + 1 + 1 + 2 (OP_PUSHDATA1 + length) + 80 = 92
        assertThat(estimatedSize).isEqualTo(92)
    }

    @Test
    fun `addMemoOutput creates correct script structure for known data`() {
        // Given
        val transaction = Transaction(networkParams)
        val memoHex = "deadbeef"

        // When
        val result = BitcoinMemoBuilder.addMemoOutput(transaction, memoHex)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)

        val output = transaction.outputs[0]
        val script = output.scriptPubKey

        // Verify script starts with OP_RETURN
        assertThat(script.chunks[0].opcode).isEqualTo(ScriptOpCodes.OP_RETURN)

        // Verify data is correctly embedded
        val dataChunk = script.chunks[1]
        assertThat(dataChunk.data).isEqualTo(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()))
    }
}