package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaAddressType
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.blockchain.blockchains.kaspa.network.*
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.*
import org.bitcoinj.core.Transaction.SigHash
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes.*
import java.math.BigDecimal
import java.math.BigInteger

class KaspaTransactionBuilder {
    private lateinit var transaction: KaspaTransaction
    private var networkParameters = KaspaMainNetParams()
    var unspentOutputs: List<KaspaUnspentOutput>? = null

    fun buildToSign(transactionData: TransactionData): Result<List<ByteArray>> {
        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Unspent outputs are missing"),
            )
        }

        val unspentsToSpend = getUnspentsToSpend()

        val change = calculateChange(
            amount = requireNotNull(transactionData.amount.value) { "Transaction amount is null" },
            fee = transactionData.fee?.amount?.value ?: BigDecimal.ZERO,
            unspentOutputs = unspentsToSpend,
        )
        if (change < BigDecimal.ZERO) { // unspentsToSpend not enough to cover transaction amount
            val maxAmount = transactionData.amount.value + change
            return Result.Failure(BlockchainSdkError.Kaspa.UtxoAmountError(MAX_INPUT_COUNT, maxAmount))
        }

        transaction = transactionData.toKaspaTransaction(networkParameters, unspentsToSpend, change)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            hashesForSign[index] = transaction.hashForSignatureWitness(
                index,
                input.scriptBytes,
                input.value,
                SigHash.ALL,
                false,
            )
        }
        return Result.Success(hashesForSign)
    }

    fun buildToSend(signatures: ByteArray): KaspaTransactionBody {
        for (index in transaction.inputs.indices) {
            val signature = extractSignature(index, signatures)
            transaction.inputs[index].scriptSig = ScriptBuilder().data(signature).build()
        }
        return KaspaTransactionBody(
            KaspaTransactionData(
                inputs = transaction.inputs.map {
                    KaspaInput(
                        previousOutpoint = KaspaPreviousOutpoint(
                            transactionId = it.outpoint.hash.toString(),
                            index = it.outpoint.index,
                        ),
                        signatureScript = it.scriptBytes.toHexString(),
                    )
                },
                outputs = transaction.outputs.map {
                    KaspaOutput(
                        amount = it.value.getValue(),
                        scriptPublicKey = KaspaScriptPublicKey(it.scriptBytes.toHexString()),
                    )
                },
            ),
        )
    }

    @Suppress("MagicNumber")
    private fun extractSignature(index: Int, signatures: ByteArray): ByteArray {
        val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s

        val canonicalSignature =
            Utils.bigIntegerToBytes(r, 32) + Utils.bigIntegerToBytes(canonicalS, 32)
        return canonicalSignature + SigHash.ALL.value.toByte()
    }

    fun calculateChange(amount: BigDecimal, fee: BigDecimal, unspentOutputs: List<KaspaUnspentOutput>): BigDecimal {
        val fullAmount = unspentOutputs.map { it.amount }.reduce { acc, number -> acc + number }
        return fullAmount - (amount + fee)
    }

    fun getUnspentsToSpendCount(): Int {
        val count = unspentOutputs?.size ?: 0
        return if (count < MAX_INPUT_COUNT) count else MAX_INPUT_COUNT
    }

    fun getUnspentsToSpend() = unspentOutputs!!.sortedByDescending { it.amount }.take(getUnspentsToSpendCount())

    companion object {
        const val MAX_INPUT_COUNT = 84 // Kaspa rejects transactions with more inputs
    }
}

@Suppress("MagicNumber")
internal fun TransactionData.toKaspaTransaction(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<KaspaUnspentOutput>,
    change: BigDecimal,
): KaspaTransaction {
    val transaction = KaspaTransaction(
        networkParameters,
    )
    transaction.setVersion(0) // the only Kaspa transaction version we know of yet
    for (utxo in unspentOutputs) {
        transaction.addInput(
            TransactionInput(
                networkParameters,
                null,
                utxo.outputScript,
                TransactionOutPoint(networkParameters, utxo.outputIndex, Sha256Hash.wrap(utxo.transactionHash)),
                Coin.parseCoin(utxo.amount.toPlainString()),
            ),
        )
    }
    for (input in transaction.inputs) {
        input.sequenceNumber = 0
    }

    val addressService = KaspaAddressService()
    val sourceScript = ScriptBuilder().data(addressService.getPublicKey(this.sourceAddress)).op(OP_CODESEPARATOR)
        .build()

    val destinationAddressDecoded = KaspaCashAddr.decodeCashAddress(this.destinationAddress)
    val destinationScript = when (destinationAddressDecoded.addressType) {
        KaspaAddressType.P2PK_SCHNORR ->
            ScriptBuilder.createP2PKOutputScript(destinationAddressDecoded.hash)
        KaspaAddressType.P2PK_ECDSA ->
            ScriptBuilder().data(destinationAddressDecoded.hash).op(OP_CODESEPARATOR).build()
        KaspaAddressType.P2SH -> {
            // known P2SH addresses won't throw
            if (destinationAddressDecoded.hash.size != 32) error("Invalid hash length in P2SH address")
            ScriptBuilder().op(OP_HASH256).data(destinationAddressDecoded.hash).op(OP_EQUAL).build()
        }
        null -> error("Null script type") // should never happen
    }

    transaction.addOutput(
        Coin.parseCoin(this.amount.value!!.toPlainString()),
        destinationScript,
    )
    if (!change.isZero()) {
        transaction.addOutput(
            Coin.parseCoin(change.toPlainString()),
            sourceScript,
        )
    }
    return transaction
}
