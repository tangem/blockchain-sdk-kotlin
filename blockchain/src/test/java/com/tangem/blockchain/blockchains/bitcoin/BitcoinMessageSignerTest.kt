package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.BitcoinMessageSigner
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
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

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

    // Test keys and addresses
    private val testPublicKey = ByteArray(65) { 0x04 }
    private val testSegwitAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
    private val testLegacyAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"

    @Before
    fun setup() {
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
        val mockSignature = ByteArray(64) { it.toByte() }

        coEvery {
            transactionSigner.sign(any<List<ByteArray>>(), any())
        } returns CompletionResult.Success(listOf(mockSignature))

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = SignMessageProtocol.ECDSA,
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
            protocol = SignMessageProtocol.ECDSA,
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
            protocol = SignMessageProtocol.BIP322,
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
            protocol = SignMessageProtocol.ECDSA,
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
            protocol = SignMessageProtocol.ECDSA,
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
        val mockSignature = ByteArray(64) { it.toByte() }

        coEvery {
            transactionSigner.sign(any<List<ByteArray>>(), any())
        } returns CompletionResult.Success(listOf(mockSignature))

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testLegacyAddress,
            protocol = SignMessageProtocol.ECDSA,
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data
        assertThat(signResult.address).isEqualTo(testLegacyAddress)

        // Header byte for Legacy (P2PKH compressed) should be 31 (0x1F)
        val signatureBytes = signResult.signature.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val headerByte = signatureBytes[0]
        assertThat(headerByte).isEqualTo(31.toByte())
    }

    @Test
    fun `signMessage with segwit address uses correct header byte`() = runTest {
        // Given
        val message = "Hello, Bitcoin!"
        val mockSignature = ByteArray(64) { it.toByte() }

        coEvery {
            transactionSigner.sign(any<List<ByteArray>>(), any())
        } returns CompletionResult.Success(listOf(mockSignature))

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = SignMessageProtocol.ECDSA,
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data

        // Header byte for Native SegWit (P2WPKH) should be 39 (0x27)
        val signatureBytes = signResult.signature.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val headerByte = signatureBytes[0]
        assertThat(headerByte).isEqualTo(39.toByte())
    }

    @Test
    fun `signMessage creates correct message hash`() = runTest {
        // Given
        val message = "Test message"
        val mockSignature = ByteArray(64) { it.toByte() }

        coEvery {
            transactionSigner.sign(any<List<ByteArray>>(), any())
        } returns CompletionResult.Success(listOf(mockSignature))

        // When
        val result = messageSigner.signMessage(
            message = message,
            address = testSegwitAddress,
            protocol = SignMessageProtocol.ECDSA,
            signer = transactionSigner,
        )

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signResult = (result as Result.Success).data

        // Message hash should be 32 bytes (64 hex characters)
        assertThat(signResult.messageHash.length).isEqualTo(64)
    }
}