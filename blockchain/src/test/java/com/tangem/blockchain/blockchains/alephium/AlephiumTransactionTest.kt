package com.tangem.blockchain.blockchains.alephium

import com.tangem.blockchain.blockchains.alephium.network.AlephiumResponse
import com.tangem.blockchain.blockchains.alephium.source.*
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import junit.framework.TestCase.assertEquals
import kotlinx.io.bytestring.ByteString
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class AlephiumTransactionTest {

    private val blockchain = Blockchain.Alephium
    private val walletPublicKey =
        (
            "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF88" +
                "82A3125B0EE9AE96DDDE1AE743F"
            ).hexToBytes()
    private val publicKeyByteString = ByteString(walletPublicKey.toCompressedPublicKey())
    private val destinationAddress = "13tVvJ9L9j2JBfmfEmYzWAonQeipBDrpwCFWGiRmGcBdE"

    private val lockupScript = LockupScript.p2pkh(publicKeyByteString)
    private val unlockScript = UnlockScript.P2PKH(publicKeyByteString)

    @Test
    fun updateCorrectUnspentOutputs() {
        val transactionBuilder = AlephiumTransactionBuilder(walletPublicKey, blockchain)
        val utxosResponse = buildUtxosResponse(false)
        transactionBuilder.updateUnspentOutputs(utxosResponse)
        val expected = buildAssetOutputInfo(utxosResponse)
        assertEquals(transactionBuilder.unspentOutputs, expected)
    }

    @Test
    fun updateCorrectUnspentOutputsWithUnconfirmed() {
        val transactionBuilder = AlephiumTransactionBuilder(walletPublicKey, blockchain)
        val utxosResponse = buildUtxosResponse(true)
        transactionBuilder.updateUnspentOutputs(utxosResponse)
        val expected = buildAssetOutputInfo(utxosResponse)
        assertEquals(transactionBuilder.unspentOutputs, expected)
    }

    @Test
    fun testSerialization() {
        val transactionBuilder = AlephiumTransactionBuilder(walletPublicKey, blockchain)
        val serialize = transactionBuilder.serializeUnsignedTransaction(createUnsignedTransaction())
            .toByteArray().toHexString()
        val expected = "00000080004E20C1174876E800035A66314FAB326ADC899B3B9B47CA506E86E400DD0BF35969" +
            "1B69201AE5A95F8DC3E525AF0003EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126F5A" +
            "66314FAB326ADC899B3B9B47CA506E86E400DD0BF359691B69201AE5A95F8DC3E525AF035A66314FAB326ADC899B3B" +
            "9B47CA506E86E400DD0BF359691B69201AE5A95F8DC3E525AF0301C444EAC6B062970000002AE8B19D653F7F6DA115" +
            "C571FDDB25F19885379D6A770B2D2A01D7C00C33F00500000000000000000000"
        assertEquals(expected, serialize)
    }

    @Test
    fun buildCorrectTransaction() {
        val transactionBuilder = AlephiumTransactionBuilder(walletPublicKey, blockchain)
        transactionBuilder.updateUnspentOutputs(buildUtxosResponse(hasUnconfirmed = false))
        val expected = createUnsignedTransaction()
        val buildToSignResult = transactionBuilder.buildToSign(
            destinationAddress = destinationAddress,
            amount = Amount(
                value = BigDecimal("4.966000000000000000"),
                blockchain = blockchain,
            ),
            fee = Fee.Alephium(
                amount = Amount(
                    value = BigDecimal("2.000000000000000"),
                    blockchain = blockchain,
                ),
                gasPrice = BigDecimal("100000000000"),
                gasAmount = BigDecimal("20000"),
            ),
        ) as Result.Success
        val unsignedTransaction = buildToSignResult.data
        assertEquals(expected, unsignedTransaction)
    }

    private fun buildUtxosResponse(hasUnconfirmed: Boolean): AlephiumResponse.Utxos = AlephiumResponse.Utxos(
        utxos = listOf(
            AlephiumResponse.Utxo(
                additionalData = null,
                amount = "1000000000000000",
                lockTime = 1739177141765,
                ref = AlephiumResponse.Utxo.Ref(
                    hint = 1516646735,
                    key = "ab326adc899b3b9b47ca506e86e400dd0bf359691b69201ae5a95f8dc3e525af",
                ),
                tokens = null,
            ),
            AlephiumResponse.Utxo(
                additionalData = null,
                amount = "3967000000000000000",
                lockTime = 1739177584937,
                ref = AlephiumResponse.Utxo.Ref(
                    hint = 1516646735,
                    key = "ab326adc899b3b9b47ca506e86e400dd0bf359691b69201ae5a95f8dc3e525af",
                ),
                tokens = null,
            ),
            AlephiumResponse.Utxo(
                additionalData = null,
                amount = "1000000000000000000",
                lockTime = 1738733626234,
                ref = AlephiumResponse.Utxo.Ref(
                    hint = 1516646735,
                    key = "ab326adc899b3b9b47ca506e86e400dd0bf359691b69201ae5a95f8dc3e525af",
                ),
                tokens = null,
            ),
            AlephiumResponse.Utxo(
                additionalData = null,
                amount = "1000000000000000000",
                lockTime = if (hasUnconfirmed) 0 else 1738733626234,
                ref = AlephiumResponse.Utxo.Ref(
                    hint = 1516646735,
                    key = "ab326adc899b3b9b47ca506e86e400dd0bf359691b69201ae5a95f8dc3e525af",
                ),
                tokens = null,
            ),
        ),
    )

    private fun buildAssetOutputInfo(utxos: AlephiumResponse.Utxos): List<AssetOutputInfo> {
        val nowMillis = System.currentTimeMillis()
        return utxos.utxos.filter { it.isNotFromFuture(nowMillis) }.map {
            AssetOutputInfo(
                ref = AssetOutputRef(
                    hint = Hint(it.ref.hint),
                    key = TxOutputRef.Key(Blake2b256(ByteString(it.ref.key.hexToBytes()))),
                ),
                outputType = UnpersistedBlockOutput,
                output = AssetOutput(
                    amount = U256.unsafe(it.amount.toBigDecimal()),
                    lockupScript = lockupScript,
                    lockTime = TimeStamp(it.lockTime),
                    tokens = listOf(),
                    additionalData = ByteString(it.additionalData?.hexToBytes() ?: byteArrayOf()),
                ),
            )
        }
    }

    private fun createUnsignedTransaction(): UnsignedTransaction {
        val decodeAddress = destinationAddress.decodeBase58(false)!!
        val decodeAddressWithoutPrefix = ByteString(decodeAddress).substring(1)
        val utxos = buildUtxosResponse(false).utxos
            .sortedByDescending { it.amount.toBigDecimal() }
            .take(3)
        return UnsignedTransaction(
            version = 0,
            networkId = NetworkId.mainNet,
            gasAmount = GasBox(20000),
            gasPrice = GasPrice(value = U256(v = BigInteger("100000000000"))),
            inputs = utxos.mapIndexed { index, utxo ->
                TxInput(
                    outputRef = AssetOutputRef(
                        hint = Hint(utxo.ref.hint),
                        key = TxOutputRef.Key(Blake2b256(ByteString(utxo.ref.key.hexToBytes()))),
                    ),
                    unlockScript = if (index == 0) unlockScript else UnlockScript.SameAsPrevious,
                )
            },
            fixedOutputs = listOf(
                AssetOutput(
                    amount = U256(BigInteger("4966000000000000000")),
                    lockupScript = LockupScript.P2PKH(Blake2b256(decodeAddressWithoutPrefix)),
                    lockTime = TimeStamp(0),
                    tokens = listOf(),
                    additionalData = ByteString(),
                ),
            ),
        )
    }
}