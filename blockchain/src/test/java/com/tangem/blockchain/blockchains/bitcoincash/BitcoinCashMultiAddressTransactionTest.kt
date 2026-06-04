package com.tangem.blockchain.blockchains.bitcoincash

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.BitcoinDynamicAddressesManager
import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptBuilder
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Multi-address transaction tests for Bitcoin Cash (Dynamic Addresses flow).
 * Verifies BCH-specific signing:
 *   - SIGHASH_ALL | SIGHASH_FORKID (0x41) byte applied per input
 *   - Per-UTXO public key used for hash and scriptSig
 *   - Change output uses the explicit changeAddress (CashAddr derived from chain=1)
 */
class BitcoinCashMultiAddressTransactionTest {

    private val blockchain = Blockchain.BitcoinCash
    private val networkParameters = MainNetParams()
    private val addressService = BitcoinCashAddressService(blockchain)

    private lateinit var accountXpub: ExtendedPublicKey
    private lateinit var walletPublicKey: ByteArray

    @Before
    fun setup() {
        CryptoUtils.initCrypto()

        // Synthetic account-level XPUB (m/44'/145'/0'). Same shape as BTC test, content is irrelevant.
        accountXpub = ExtendedPublicKey(
            publicKey = "0339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2".hexToByteArray(),
            chainCode = "873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d508".hexToByteArray(),
            depth = 3,
        )
        walletPublicKey = accountXpub.publicKey
    }

    @Test
    fun buildToSignMultiAddress_twoInputs_returnsHashesWithFORKIDPaths() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)
        val addr5 = manager.deriveAddress(chain = 0, index = 5)

        val utxos = createMultiAddressUtxos(
            derivedAddresses = listOf(addr0, addr5),
            amounts = listOf("0.001".toBigDecimal(), "0.002".toBigDecimal()),
        )

        val builder = BitcoinCashTransactionBuilder(walletPublicKey, blockchain)
        builder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()
        val changeAddress = manager.deriveAddress(chain = 1, index = 0).address
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val result = builder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = changeAddress,
        )

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val signatureInfos = (result as Result.Success).data
        assertThat(signatureInfos).isNotEmpty()
        for (info in signatureInfos) {
            assertThat(info.hash).isNotEmpty()
            assertThat(info.publicKey.size).isEqualTo(33) // compressed
            assertThat(info.derivationPath).isNotNull()
        }
    }

    @Test
    fun buildToSignMultiAddress_changeGoesToSpecifiedCashAddrAddress() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)
        val changeAddress = manager.deriveAddress(chain = 1, index = 0).address

        val utxos = createMultiAddressUtxos(
            derivedAddresses = listOf(addr0),
            amounts = listOf("0.01".toBigDecimal()),
        )

        val builder = BitcoinCashTransactionBuilder(walletPublicKey, blockchain)
        builder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val result = builder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = changeAddress,
        ) as Result.Success

        assertThat(result.data).hasSize(1)

        val tx = builder.getTransaction()
        assertThat(tx.outputs.size).isEqualTo(2)

        // Change output (index 1) — script should hash to the changeAddress's pubkey hash
        val expectedHash = addressService.getPublicKeyHash(changeAddress)
        val expectedLegacy = LegacyAddress.fromPubKeyHash(networkParameters, expectedHash)
        val changeOutputAddress = tx.outputs[1].scriptPubKey.getToAddress(networkParameters).toString()
        assertThat(changeOutputAddress).isEqualTo(expectedLegacy.toString())
    }

    @Test
    fun buildToSendMultiAddress_appendsForkIdByteToEverySignature() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)
        val addr1 = manager.deriveAddress(chain = 0, index = 1)

        val utxos = createMultiAddressUtxos(
            derivedAddresses = listOf(addr0, addr1),
            amounts = listOf("0.001".toBigDecimal(), "0.001".toBigDecimal()),
        )

        val builder = BitcoinCashTransactionBuilder(walletPublicKey, blockchain)
        builder.unspentOutputs = utxos

        val sendAmount = "0.001".toBigDecimal()
        val feeAmount = "0.0002".toBigDecimal()
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val buildResult = builder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = addr0.address,
        ) as Result.Success
        assertThat(buildResult.data).hasSize(2)

        // 64-byte canonical-shaped signatures (r,s in (0, n/2))
        val signatures = List(2) { idx ->
            val seed = (idx + 1).toByte()
            ByteArray(64) { i -> if (i == 0) seed else 0x01 }
        }

        val txBytes = builder.buildToSendMultiAddress(signatures, buildResult.data)
        assertThat(txBytes).isNotEmpty()

        // Each input scriptSig must end the DER signature with sighash byte 0x41 (SIGHASH_ALL | FORKID).
        // scriptSig layout: <pushSigLen> <DER...|0x41> <pushPubKeyLen> <33-byte pubKey>
        val tx = builder.getTransaction()
        for (input in tx.inputs) {
            val scriptBytes = input.scriptSig.program
            // Find pubkey push at the end (33-byte compressed pubkey + 1 length byte = last 34 bytes).
            val pubKeyPushLen = 1 + 33
            val sigPushEnd = scriptBytes.size - pubKeyPushLen
            // Last byte of the signature push is the sighash byte.
            val sigHashByte = scriptBytes[sigPushEnd - 1].toInt() and 0xFF
            assertThat(sigHashByte).isEqualTo(0x41)
        }
    }

    @Test
    fun buildToSignMultiAddress_singleInput_publicKeyMatchesUtxo() {
        val manager = BitcoinDynamicAddressesManager(accountXpub, blockchain)
        val addr0 = manager.deriveAddress(chain = 0, index = 0)

        val utxos = createMultiAddressUtxos(
            derivedAddresses = listOf(addr0),
            amounts = listOf("0.01".toBigDecimal()),
        )

        val builder = BitcoinCashTransactionBuilder(walletPublicKey, blockchain)
        builder.unspentOutputs = utxos

        val sendAmount = "0.009".toBigDecimal()
        val feeAmount = "0.0001".toBigDecimal()
        val transactionData = TransactionData.Uncompiled(
            sourceAddress = addr0.address,
            destinationAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
            amount = Amount(sendAmount, blockchain, AmountType.Coin),
            fee = Fee.Common(Amount(Amount(sendAmount, blockchain, AmountType.Coin), feeAmount)),
        )

        val result = builder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = addr0.address,
        ) as Result.Success

        assertThat(result.data).hasSize(1)
        assertThat(result.data[0].publicKey.toList()).isEqualTo(addr0.publicKey.toList())
    }

    @Test
    fun buildToSignMultiAddress_emptyUtxos_returnsFailure() {
        val builder = BitcoinCashTransactionBuilder(walletPublicKey, blockchain)
        builder.unspentOutputs = emptyList()

        val transactionData = TransactionData.Uncompiled(
            sourceAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
            destinationAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
            amount = Amount("0.001".toBigDecimal(), blockchain, AmountType.Coin),
            fee = Fee.Common(
                Amount(Amount("0.001".toBigDecimal(), blockchain, AmountType.Coin), "0.0001".toBigDecimal()),
            ),
        )

        val result = builder.buildToSignMultiAddress(
            transactionData = transactionData,
            dustValue = "0.00001".toBigDecimal(),
            changeAddress = "bitcoincash:qrcuqadqrzp2uztjl9wn5sthepkg22majyxw4gmv6p",
        )

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    private fun createMultiAddressUtxos(
        derivedAddresses: List<DynamicAddressesManager.DerivedAddress>,
        amounts: List<BigDecimal>,
    ): List<BitcoinUnspentOutput> {
        return derivedAddresses.zip(amounts).mapIndexed { i, (addr, amount) ->
            val pubKeyHash = addressService.getPublicKeyHash(addr.address)
            val legacyAddress = LegacyAddress.fromPubKeyHash(networkParameters, pubKeyHash)
            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = i.toLong(),
                transactionHash = ByteArray(32) { (i + 1).toByte() },
                outputScript = ScriptBuilder.createOutputScript(legacyAddress).program,
                address = addr.address,
                derivationPath = "m/44'/145'/0'/${addr.chain}/${addr.index}",
                publicKey = addr.publicKey,
            )
        }
    }

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}