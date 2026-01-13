package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.eip7702.EthEip7702Util
import com.tangem.common.extensions.toHexString
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigInteger

/**
 * Tests for EIP-7702 authorization encoding according to the specification.
 * EIP-7702: Set EOA account code for one transaction
 */
class EthEip7702Test {

    @Test
    fun testBasicAuthorizationEncoding() {
        // Test basic authorization with known values
        val chainId = 1 // Ethereum mainnet
        val contractAddress = "0x1234567890123456789012345678901234567890"
        val nonce = BigInteger.ZERO

        val hash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        // Verify that hash is 32 bytes (keccak256 output)
        assertEquals(32, hash.size, "Hash should be 32 bytes")

        // Verify hash is not all zeros
        assert(hash.any { it != 0.toByte() }) { "Hash should not be all zeros" }
    }

    @Test
    fun testAuthorizationWithNonZeroNonce() {
        val chainId = 1
        val contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
        val nonce = BigInteger.valueOf(42)

        val hash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        assertEquals(32, hash.size, "Hash should be 32 bytes")
    }

    @Test
    fun testAuthorizationWithLargeChainId() {
        val chainId = 137 // Polygon mainnet
        val contractAddress = "0x0000000000000000000000000000000000000001"
        val nonce = BigInteger.ONE

        val hash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        assertEquals(32, hash.size, "Hash should be 32 bytes")
    }

    @Test
    fun testAuthorizationWithLargeNonce() {
        val chainId = 1
        val contractAddress = "0xffffffffffffffffffffffffffffffffffffffff"
        val nonce = BigInteger("999999999999999999999999")

        val hash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        assertEquals(32, hash.size, "Hash should be 32 bytes")
    }

    @Test
    fun testDifferentAddressesProduceDifferentHashes() {
        val chainId = 1
        val nonce = BigInteger.ZERO

        val hash1 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = "0x1111111111111111111111111111111111111111",
            nonce = nonce,
        )

        val hash2 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = "0x2222222222222222222222222222222222222222",
            nonce = nonce,
        )

        assert(!hash1.contentEquals(hash2)) {
            "Different addresses should produce different hashes"
        }
    }

    @Test
    fun testDifferentNoncesProduceDifferentHashes() {
        val chainId = 1
        val contractAddress = "0x1234567890123456789012345678901234567890"

        val hash1 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = BigInteger.ZERO,
        )

        val hash2 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = BigInteger.ONE,
        )

        assert(!hash1.contentEquals(hash2)) {
            "Different nonces should produce different hashes"
        }
    }

    @Test
    fun testDifferentChainIdsProduceDifferentHashes() {
        val contractAddress = "0x1234567890123456789012345678901234567890"
        val nonce = BigInteger.ZERO

        val hash1 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = 1,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        val hash2 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = 137,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        assert(!hash1.contentEquals(hash2)) {
            "Different chain IDs should produce different hashes"
        }
    }

    @Test
    fun testSameParametersProduceSameHash() {
        val chainId = 1
        val contractAddress = "0x1234567890123456789012345678901234567890"
        val nonce = BigInteger.valueOf(100)

        val hash1 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        val hash2 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddress,
            nonce = nonce,
        )

        assert(hash1.contentEquals(hash2)) {
            "Same parameters should produce the same hash"
        }
    }

    @Test
    fun testAddressWithoutPrefix() {
        val chainId = 1
        val contractAddressWithPrefix = "0x1234567890123456789012345678901234567890"
        val contractAddressWithoutPrefix = "1234567890123456789012345678901234567890"
        val nonce = BigInteger.ZERO

        val hash1 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddressWithPrefix,
            nonce = nonce,
        )

        val hash2 = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = contractAddressWithoutPrefix,
            nonce = nonce,
        )

        assert(hash1.contentEquals(hash2)) {
            "Address with and without 0x prefix should produce the same hash"
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidAddressLength() {
        val chainId = 1
        val invalidAddress = "0x1234" // Too short
        val nonce = BigInteger.ZERO

        EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = invalidAddress,
            nonce = nonce,
        )
    }

    @Test
    fun testRealWorldScenario() {
        val expectedHash = "05BFD67D791161EC62068619AC58BA0F7900E29432C6D5A431811CFA7C93EF0C"
        // Simulate real-world gasless contract authorization
        val chainId = 137 // Polygon mainnet
        val gaslessContractAddress = "0x2Bfd00f7D053E7a665d1767f08c5a57B3F52Ec89"
        val userNonce = BigInteger.valueOf(5)

        val authHash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = gaslessContractAddress,
            nonce = userNonce,
        )

        // Verify the hash
        val hexHash = authHash.toHexString()
        assertEquals(expectedHash, hexHash)
    }

    @Test
    fun testZeroAddress() {
        val chainId = 1
        val zeroAddress = "0x0000000000000000000000000000000000000000"
        val nonce = BigInteger.ZERO

        val hash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = zeroAddress,
            nonce = nonce,
        )

        assertEquals(32, hash.size, "Hash should be 32 bytes even for zero address")
    }

    @Test
    fun testMaxAddress() {
        val chainId = 1
        val maxAddress = "0xffffffffffffffffffffffffffffffffffffffff"
        val nonce = BigInteger.ZERO

        val hash = EthEip7702Util.encodeAuthorizationForSigning(
            chainId = chainId,
            contractAddress = maxAddress,
            nonce = nonce,
        )

        assertEquals(32, hash.size, "Hash should be 32 bytes even for max address")
    }

    @Test
    fun testDeterministicEncoding() {
        // Test that the encoding is deterministic across multiple calls
        val chainId = 11155111 // Sepolia testnet
        val contractAddress = "0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF"
        val nonce = BigInteger.valueOf(12345)

        val hashes = List(10) {
            EthEip7702Util.encodeAuthorizationForSigning(
                chainId = chainId,
                contractAddress = contractAddress,
                nonce = nonce,
            )
        }

        // All hashes should be identical
        hashes.forEach { hash ->
            assert(hash.contentEquals(hashes[0])) {
                "Encoding should be deterministic"
            }
        }
    }
}