package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.alt.SolanaTransactionParser
import com.tangem.blockchain.extensions.encodeBase58
import foundation.metaplex.solana.transactions.AccountMeta
import foundation.metaplex.solana.transactions.CompiledInstruction
import foundation.metaplex.solanapublickeys.PublicKey
import io.ktor.util.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.collections.isNotEmpty

class SolanaTransactionConverterTest {

    private val converter = SolanaTransactionParser()

    @Test
    fun `should parse legacy solana transaction from base64`() {
        val base64Tx = """
            AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAUMMs46T+b22EVEJz+YL3KyQnmwDSeubb+kXIbssdrBP0cSY04PqyhUSpaSmXwmD5AGJGw5jQFoxCpqCoDHyRUFxSt8eazGux7vKsP9SOUVlW6RvjKOHgzob4NNeNuvzhfpZS6g1K0EOXLfR5+hmmDTxEzxqKoayyYGcYnfiZ1Qsq6qtCREEs4S0wdW7gwtgKM/JO72XtKjcayVwSlxrklWXcqVbTpWrpoggVbym3PrMTNp5yhXSpKzGlSXQRPmHEgpzAf6e09X6pdgO8CGfg/Drrn4SDIuIS/8IkJj9opduXUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMGRm/lIRcy/+ytunLDm+e8jOW7xfcSayxDmzpAAAAAvOlgzG/+eRqMo2MgDHjNi0Wsr5/o0CTAgpYgGPRF4bIG3fbh12Whk9nL4UbO63msHLSF7V9bN5E6jPWFfv8AqQ3IUXC4UD+LVokSAAMPIAbwFmTcAF0bSlZfZiEI1/9kOLP46LH+8zEXtumAIS0BoHQ9Zp749xF0NoKnfne2xuYDCQsDBQYAAAoHCwQBAtwDMm/ydjP6jbvZnSMBAAAAAAAAAAAAAAAADgAAAIXVCpv3Z0lk7qnryvKmehZePlNf3eWAVAM+bbs/5oZYh8kuHXPtOuasE9/L/bxWnQ1zrbWurRKchhqbv2sBgeUvXFFQygP8CzjBB+Iihl4zxE8wILPFPMszsmKBNOPTWDFIYKzAufJa7Fxape/MuVh+ZlQjpic/4m058tr8x2nPzPSk9t+DpuU9Dlr/R41/2Y1zhiZsS9462lAkycKY4ASXc0HLM5BfAiorKrQxFXdydcGgh0kh8C3/NzTBbbRhMKCSiLB87SH4lTEqpzxvT0fMLUJCkXKPE/DGKiot2sLD1rIuO7Gtc8rkprGPOrUw5bMLv2uCUeLOwJtTr6go36ejCm4w9Tj60rcMISxUIYLTpKwS8CL3xHg6/KxdK1tTMGpR0KZNLoegmWXHmOUEHqoyYsuOYEavhiQ1R9u89T4vLrGH9blZ8ATCVRPGalkZufqN7r/3fN7u28JPUu4YQ8LMKos3HU2IOwMU2G8nQDmzZMphUCXLIPL9vPbmKzfxM44O5lT4Fk0CPB5uncI0NNYGrq+aNwHK95sykF6YRd+7yk+iydYOYPv+S4V2lpAA6+ofnzMesj01lCVrrem2+bIIAAUC8EkCAAgACQNxtwIAAAAAAA==
        """.trimIndent().replace("\n", "")
        val txBytes = SolanaTransactionHelper.removeSignaturesPlaceholders(base64Tx.decodeBase64Bytes())

        val parsed = converter.parse(txBytes)

        assertNotNull(parsed)
        assertEquals(32, parsed.payer.size)
        assert(parsed.staticAccountAddresses.isNotEmpty())
        assert(parsed.writableAltAddresses.isNotEmpty() || parsed.readonlyAltAddresses.isNotEmpty())
        assert(parsed.compiledInstructions.isNotEmpty())
        assertEquals(32, parsed.recentBlockhash.size)

        println("all account addresses: ${parsed.staticAccountAddresses.joinToString(", ") { it.encodeBase58() }}\n")
        println("writeable alt addresses: ${parsed.writableAltAddresses.joinToString(", ") { it.encodeBase58() }}\n")
        println(
            "readonlyAltAddresses alt addresses: ${parsed.readonlyAltAddresses.joinToString(
                ", ",
            ) { it.encodeBase58() }}\n",
        )

        val writableAltAddressesBase58 = listOf(
            "2EnAXqk5GSvwmQkeRRwXbwN12PG9F4tLozKkhYo2Y1BS",
            "3vkcpYxXK6J3YmbhTpj8jnWWrzPXDMVJxNFJadb7JzvU",
            "7oyR7WyENjzU3GCnLFFmeSHHDD9wFybpqLfCXPZQEsS5",
            "CVMdMd79no569tjc5Sq7kzz8isbfCcFyBS5TLGsrZ5dN",
            "EdoWW7Q6dNtx5qNTfsgf8z9mvz8Ue1i9f6dPD4z4A7Fn",
            "EjTE91hujZjLF7UAGDUA6853QNMhnZTXezmMTfpyqtiY",
        )

        val readonlyAltAddressesBase58 = listOf(
            "11111111111111111111111111111111",
            "ComputeBudget111111111111111111111111111111",
            "DiS3nNjFVMieMgmiQFm6wgJL7nevk4NrhXKLbtEH1Z2R",
            "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA",
            "voTpe3tHQ7AjQHMapgSue2HJFAh2cGsdokqN3XqmVSj",
        )

        val parsedWritableBase58 = parsed.writableAltAddresses.map { it.encodeBase58() }
        val parsedReadonlyBase58 = parsed.readonlyAltAddresses.map { it.encodeBase58() }

        assertEquals(writableAltAddressesBase58, parsedWritableBase58)
        assertEquals(readonlyAltAddressesBase58, parsedReadonlyBase58)
    }

    @Test
    fun `convertCompiledToTransactionInstructions derives all four account privilege categories`() {
        // Solana account ordering: [writable signers | readonly signers | writable non-signers | readonly non-signers]
        // requiredSignatures = 2, readonlySigned = 1, readonlyUnsigned = 1, total = 5
        //   idx 0 -> writable signer
        //   idx 1 -> readonly signer
        //   idx 2 -> writable non-signer
        //   idx 3 -> writable non-signer
        //   idx 4 -> readonly non-signer
        val accountAddresses = (0 until 5).map { address(seed = it) }
        val instructionData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val compiled = CompiledInstruction(
            programIdIndex = 4,
            accounts = listOf(0, 1, 2, 3, 4),
            data = instructionData.encodeBase58(),
        )

        val result = converter.convertCompiledToTransactionInstructions(
            compiledInstructions = listOf(compiled),
            allAccountAddresses = accountAddresses,
            requiredSignatures = 2,
            readonlySignedAccounts = 1,
            readonlyUnsignedAccounts = 1,
        )

        assertEquals(1, result.size)
        val instruction = result.first()
        assertEquals(PublicKey(address(seed = 4)), instruction.programId)
        assertArrayEquals(instructionData, instruction.data)

        val expectedKeys = listOf(
            AccountMeta(PublicKey(address(seed = 0)), isSigner = true, isWritable = true),
            AccountMeta(PublicKey(address(seed = 1)), isSigner = true, isWritable = false),
            AccountMeta(PublicKey(address(seed = 2)), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(address(seed = 3)), isSigner = false, isWritable = true),
            AccountMeta(PublicKey(address(seed = 4)), isSigner = false, isWritable = false),
        )
        assertEquals(expectedKeys, instruction.keys)
    }

    @Test
    fun `convertCompiledToTransactionInstructions preserves instruction order and maps each program id`() {
        val accountAddresses = (0 until 3).map { address(seed = it) }
        val first = CompiledInstruction(
            programIdIndex = 1,
            accounts = listOf(0),
            data = byteArrayOf(0x0A).encodeBase58(),
        )
        val second = CompiledInstruction(
            programIdIndex = 2,
            accounts = listOf(0),
            data = byteArrayOf(0x0B).encodeBase58(),
        )

        val result = converter.convertCompiledToTransactionInstructions(
            compiledInstructions = listOf(first, second),
            allAccountAddresses = accountAddresses,
            requiredSignatures = 1,
            readonlySignedAccounts = 0,
            readonlyUnsignedAccounts = 1,
        )

        assertEquals(2, result.size)
        assertEquals(PublicKey(address(seed = 1)), result[0].programId)
        assertEquals(PublicKey(address(seed = 2)), result[1].programId)
        assertArrayEquals(byteArrayOf(0x0A), result[0].data)
        assertArrayEquals(byteArrayOf(0x0B), result[1].data)
    }

    @Test
    fun `convertCompiledToTransactionInstructions returns empty list for empty input`() {
        val result = converter.convertCompiledToTransactionInstructions(
            compiledInstructions = emptyList(),
            allAccountAddresses = listOf(address(seed = 0)),
            requiredSignatures = 1,
            readonlySignedAccounts = 0,
            readonlyUnsignedAccounts = 0,
        )

        assertTrue(result.isEmpty())
    }

    /** Builds a distinct 32-byte account address filled with [seed], so each index maps to a unique [PublicKey]. */
    private fun address(seed: Int): ByteArray = ByteArray(size = 32) { seed.toByte() }
}