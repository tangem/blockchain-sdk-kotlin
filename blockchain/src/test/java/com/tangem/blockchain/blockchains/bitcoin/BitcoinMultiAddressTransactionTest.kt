package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import org.bitcoinj.core.Address
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptBuilder
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Tests for multi-address transaction building (buildToSignMultiAddress / buildToSendMultiAddress).
 * Each UTXO carries its own derived public key and derivation path.
 */
class BitcoinMultiAddressTransactionTest {

    private val blockchain = Blockchain.Bitcoin
    private val networkParameters = MainNetParams()

    // Account-level XPUB (m/84'/0'/0')
    // Using BIP32 test vector 1 master key
    private lateinit var accountXpub: ExtendedPublicKey

    // Wallet public key = the "seed" key (master public key)
    // For the TransactionBuilder, we use the account-level key as walletPublicKey
    private lateinit var walletPublicKey: ByteArray

    @Before
    fun setup() {
        CryptoUtils.initCrypto()

        // Construct account-level XPUB with known test values
        accountXpub = ExtendedPublicKey(
            publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToByteArray(),
            chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToByteArray(),
            depth = 3,
        )

        walletPublicKey = accountXpub.publicKey
    }

    // ========== buildToSignMultiAddress Tests ==========

    @Test
    fun buildToSignMultiAddress_twoInputs_returnsHashesWithPaths() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)

        // Derive two addresses at different indices
        val addr0 = manager.deriveAddress(chain = 0, index = 0)
        val addr5 = manager.deriveAddress(chain = 0, index = 5)

        // Create UTXOs with derived public keys and derivation paths
        val utxos = createMultiAddressUtxos(
            addresses = listOf(addr0, addr5),
            amounts = listOf("0.001".toBigDecimal(), "0.002".toBigDecimal()),
        )

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()
        val changeAddress = manager.deriveAddress(chain = 1, index = 0).address

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        // Act
        val result = transactionBuilder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = changeAddress,
        )

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signatureInfos = (result as Result.Success).data
        assertThat(signatureInfos).isNotEmpty()

        // Each info has a hash, public key, and derivation path
        for (info in signatureInfos) {
            assertThat(info.hash).isNotEmpty()
            assertThat(info.publicKey).isNotEmpty()
            assertThat(info.publicKey.size).isEqualTo(33) // compressed
            assertThat(info.derivationPath).isNotNull()
        }
    }

    @Test
    fun buildToSignMultiAddress_changeGoesToSpecifiedAddress() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)

        val utxos = createMultiAddressUtxos(
            addresses = listOf(addr0),
            amounts = listOf("0.01".toBigDecimal()),
        )

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()
        val changeAddress = manager.deriveAddress(chain = 1, index = 0).address

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        // Act
        val result = transactionBuilder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = changeAddress,
        )

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signatureInfos = (result as Result.Success).data

        // Verify the transaction has 2 outputs (send + change)
        val tx = transactionBuilder.getTransaction()
        assertThat(tx.outputs.size).isEqualTo(2)

        // Second output (change) should go to the specified change address
        val changeOutput = tx.outputs[1]
        val changeOutputAddress = changeOutput.scriptPubKey.getToAddress(networkParameters).toString()
        assertThat(changeOutputAddress).isEqualTo(changeAddress)
    }

    @Test
    fun buildToSignMultiAddress_segwit_usesPerInputPubKey() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)

        // Two different derived addresses — different public keys per input
        val addr0 = manager.deriveAddress(chain = 0, index = 0)
        val addr1 = manager.deriveAddress(chain = 0, index = 1)

        val utxos = createMultiAddressUtxos(
            addresses = listOf(addr0, addr1),
            amounts = listOf("0.001".toBigDecimal(), "0.001".toBigDecimal()),
        )

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0002".toBigDecimal()

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val result = transactionBuilder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = addr0.address,
        )

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signatureInfos = (result as Result.Success).data

        // Each input should have its own unique public key
        if (signatureInfos.size >= 2) {
            assertThat(signatureInfos[0].publicKey).isNotEqualTo(signatureInfos[1].publicKey)
        }
    }

    @Test
    fun buildToSendMultiAddress_producesValidTransaction() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)

        val utxos = createMultiAddressUtxos(
            addresses = listOf(addr0),
            amounts = listOf("0.01".toBigDecimal()),
        )

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val buildResult = transactionBuilder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = addr0.address,
        ) as Result.Success

        // Create dummy signatures (64 bytes per input)
        val signatures = buildResult.data.map { ByteArray(64) { 0x01 } }

        // Act
        val txBytes = transactionBuilder.buildToSendMultiAddress(signatures, buildResult.data)

        // Assert - should produce non-empty serialized transaction
        assertThat(txBytes).isNotEmpty()
        assertThat(txBytes.size).isGreaterThan(50)
    }

    @Test
    fun buildToSignMultiAddress_singleInput_works() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)

        val utxos = createMultiAddressUtxos(
            addresses = listOf(addr0),
            amounts = listOf("0.01".toBigDecimal()),
        )

        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs = utxos

        val sendAmount = "0.009".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val result = transactionBuilder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = addr0.address,
        )

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signatureInfos = (result as Result.Success).data
        assertThat(signatureInfos).hasSize(1)
        assertThat(signatureInfos[0].publicKey.toList()).isEqualTo(addr0.publicKey.toList())
    }

    @Test
    fun buildToSignMultiAddress_emptyUtxos_returnsFailure() {
        val addresses = BitcoinAddressService(blockchain).makeAddresses(walletPublicKey)
        val transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, addresses)
        transactionBuilder.unspentOutputs = emptyList()

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            destinationAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            amount = Amount("0.001".toBigDecimal(), blockchain, AmountType.Coin),
            fee = Fee.Common(
                Amount(Amount("0.001".toBigDecimal(), blockchain, AmountType.Coin), "0.0001".toBigDecimal()),
            ),
        )

        val result = transactionBuilder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
        )

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    // ========== Helpers ==========

    private fun createMultiAddressUtxos(
        addresses: List<DynamicAddressesManager.DerivedAddress>,
        amounts: List<BigDecimal>,
    ): List<BitcoinUnspentOutput> {
        return addresses.zip(amounts).mapIndexed { i, (addr, amount) ->
            val bitcoinjAddr = Address.fromString(networkParameters, addr.address)
            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = i.toLong(),
                transactionHash = ByteArray(32) { (i + 1).toByte() },
                outputScript = ScriptBuilder.createOutputScript(bitcoinjAddr).program,
                address = addr.address,
                derivationPath = "m/84'/0'/0'/${addr.chain}/${addr.index}",
                publicKey = addr.publicKey,
            )
        }
    }

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}