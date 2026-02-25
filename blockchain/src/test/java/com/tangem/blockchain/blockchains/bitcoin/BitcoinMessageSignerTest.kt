package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.messagesigning.BitcoinMessageSigner
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageProtocol
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

/**
 * Unit tests for BitcoinMessageSigner.
 *
 * Tests cover:
 * - Message signing with ECDSA
 * - Address validation
 * - Signature format verification
 * - Error handling
 */
class BitcoinMessageSignerTest {

    private lateinit var wallet: Wallet
    private lateinit var messageSigner: BitcoinMessageSigner
    private lateinit var transactionSigner: TransactionSigner
    private lateinit var testECKey: ECKey

    // Test keys and addresses
    private lateinit var testPublicKey: ByteArray
    private lateinit var testSegwitAddress: String
    private lateinit var testLegacyAddress: String

    @Before
    fun setup() {
        // Generate a deterministic key for testing using a known private key
        val privateKeyBigInt = BigInteger("18e14a7b6a307f426a94f8114701e7c8e774e7f9a47e2c2035db29a206321725", 16)
        testECKey = ECKey.fromPrivate(privateKeyBigInt, true)

        // Get uncompressed public key (65 bytes: 0x04 + 32 bytes X + 32 bytes Y)
        testPublicKey = testECKey.pubKeyPoint.getEncoded(false)

        // Generate Bitcoin addresses for testing
        // Using bitcoinj mainnet addresses
        val params = org.bitcoinj.params.MainNetParams.get()

        // Legacy P2PKH address
        testLegacyAddress = org.bitcoinj.core.LegacyAddress.fromKey(params, testECKey).toString()

        // Native SegWit P2WPKH address
        testSegwitAddress = org.bitcoinj.core.SegwitAddress.fromKey(params, testECKey).toBech32()

        // Setup wallet with multiple address types
        wallet = Wallet(
            blockchain = Blockchain.Bitcoin,
            addresses = setOf(
                Address(testSegwitAddress, AddressType.Default),
                Address(testLegacyAddress, AddressType.Legacy),
            ),
            publicKey = Wallet.PublicKey(
                seedKey = testPublicKey.copyOf(),
                derivationType = null,
            ),
            tokens = emptySet(),
        )

        // Create message signer
        messageSigner = BitcoinMessageSigner(wallet)

        // Mock transaction signer
        transactionSigner = mockk()
    }

    @Test
    fun `signMessage succeeds with valid address`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"

        // Capture the hash to sign and return a real signature
        val hashSlot = slot<List<ByteArray>>()
        coEvery {
            transactionSigner.sign(capture(hashSlot), any())
        } answers {
            val hash = hashSlot.captured.first()
            val ecdsaSignature = testECKey.sign(Sha256Hash.wrap(hash))
            // Return r and s components (32 bytes each)
            val signature = ecdsaSignature.r.toByteArray().takeLast(32).toByteArray() +
                ecdsaSignature.s.toByteArray().takeLast(32).toByteArray()
            CompletionResult.Success(listOf(signature))
        }

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = "ecdsa",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data
        assertThat(signResult.address).isEqualTo(testSegwitAddress)
        assertThat(signResult.signature).isNotEmpty()
        assertThat(signResult.messageHash).isNotEmpty()

        // Signature should be 65 bytes (130 hex characters: header + r + s)
        assertThat(signResult.signature.length).isEqualTo(130)
    }

    @Test
    fun `signMessage fails with address not in wallet`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"
        val wrongAddress = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = wrongAddress,
            protocol = "ecdsa",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        val error = (result as Result.Failure).error
        assertThat(error.customMessage).contains("does not belong to this wallet")
    }

    @Test
    fun `signMessage fails with BIP322 protocol`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = "bip322",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        val error = (result as Result.Failure).error
        assertThat(error.customMessage).contains("BIP322")
        assertThat(error.customMessage).contains("not yet supported")
    }

    @Test
    fun `signMessage handles signing failure from signer`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"

        // Mock signer failure
        coEvery {
            transactionSigner.sign(any<List<ByteArray>>(), any())
        } returns CompletionResult.Failure(mockk(relaxed = true))

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = "ecdsa",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `signMessage fails with invalid signature size`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"
        val invalidSignature = ByteArray(32) { it.toByte() } // Wrong size (should be 64)

        coEvery {
            transactionSigner.sign(any<List<ByteArray>>(), any())
        } returns CompletionResult.Success(listOf(invalidSignature))

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = "ecdsa",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        val error = (result as Result.Failure).error
        assertThat(error.customMessage).contains("Invalid signature size")
    }

    @Test
    fun `signMessage with legacy address uses correct header byte`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"

        // Capture the hash to sign and return a real signature
        val hashSlot = slot<List<ByteArray>>()
        coEvery {
            transactionSigner.sign(capture(hashSlot), any())
        } answers {
            val hash = hashSlot.captured.first()
            val ecdsaSignature = testECKey.sign(Sha256Hash.wrap(hash))
            // Return r and s components (32 bytes each)
            val signature = ecdsaSignature.r.toByteArray().takeLast(32).toByteArray() +
                ecdsaSignature.s.toByteArray().takeLast(32).toByteArray()
            CompletionResult.Success(listOf(signature))
        }

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testLegacyAddress,
            protocol = SignMessageProtocol.ECDSA.name,
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data
        assertThat(signResult.address).isEqualTo(testLegacyAddress)

        // Header byte for Legacy (P2PKH compressed) should be 31-34 (base 31 + recovery ID 0-3)
        val signatureBytes = signResult.signature.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val headerByte = signatureBytes[0].toInt()
        assertThat(headerByte).isAtLeast(31)
        assertThat(headerByte).isAtMost(34)
    }

    @Test
    fun `signMessage with segwit address uses correct header byte`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"

        // Capture the hash to sign and return a real signature
        val hashSlot = slot<List<ByteArray>>()
        coEvery {
            transactionSigner.sign(capture(hashSlot), any())
        } answers {
            val hash = hashSlot.captured.first()
            val ecdsaSignature = testECKey.sign(Sha256Hash.wrap(hash))
            // Return r and s components (32 bytes each)
            val signature = ecdsaSignature.r.toByteArray().takeLast(32).toByteArray() +
                ecdsaSignature.s.toByteArray().takeLast(32).toByteArray()
            CompletionResult.Success(listOf(signature))
        }

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = "ecdsa",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data

        // Header byte for Native SegWit (P2WPKH) should be 39-42 (base 39 + recovery ID 0-3)
        val signatureBytes = signResult.signature.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val headerByte = signatureBytes[0].toInt()
        assertThat(headerByte).isAtLeast(39)
        assertThat(headerByte).isAtMost(42)
    }

    @Test
    fun `signMessage creates correct message hash`() = runTest {
        // Given
        val message = "Test message"

        // Capture the hash to sign and return a real signature
        val hashSlot = slot<List<ByteArray>>()
        coEvery {
            transactionSigner.sign(capture(hashSlot), any())
        } answers {
            val hash = hashSlot.captured.first()
            val ecdsaSignature = testECKey.sign(Sha256Hash.wrap(hash))
            // Return r and s components (32 bytes each)
            val signature = ecdsaSignature.r.toByteArray().takeLast(32).toByteArray() +
                ecdsaSignature.s.toByteArray().takeLast(32).toByteArray()
            CompletionResult.Success(listOf(signature))
        }

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = "ecdsa",
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data

        // Message hash should be 32 bytes (64 hex characters)
        assertThat(signResult.messageHash.length).isEqualTo(64)
    }
}