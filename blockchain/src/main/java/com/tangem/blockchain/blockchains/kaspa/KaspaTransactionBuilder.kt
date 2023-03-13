package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaAddressType
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.blockchain.blockchains.kaspa.network.KaspaInput
import com.tangem.blockchain.blockchains.kaspa.network.KaspaOutput
import com.tangem.blockchain.blockchains.kaspa.network.KaspaPreviousOutpoint
import com.tangem.blockchain.blockchains.kaspa.network.KaspaScriptPublicKey
import com.tangem.blockchain.blockchains.kaspa.network.KaspaTransactionBody
import com.tangem.blockchain.blockchains.kaspa.network.KaspaTransactionData
import com.tangem.blockchain.blockchains.kaspa.network.KaspaUnspentOutput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.*
import org.bitcoinj.core.Transaction.SigHash
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes.OP_CODESEPARATOR
import java.math.BigDecimal
import java.math.BigInteger

class KaspaTransactionBuilder() {
    private lateinit var transaction: KaspaTransaction
    private var networkParameters = MainNetParams()
    var unspentOutputs: List<KaspaUnspentOutput>? = null

    fun buildToSign(
            transactionData: TransactionData): Result<List<ByteArray>> {

        if (unspentOutputs.isNullOrEmpty()) return Result.Failure(BlockchainSdkError.CustomError("Unspent outputs are missing"))

        val change: BigDecimal = calculateChange(transactionData, unspentOutputs!!)

        transaction = transactionData.toKaspaTransaction(networkParameters, unspentOutputs!!, change)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            val value = Coin.parseCoin(unspentOutputs!![index].amount.toString())
            hashesForSign[index] = getTransaction().hashForSignatureWitness(
                    index,
                    input.scriptBytes,
                    value,
                    Transaction.SigHash.ALL,
                    false
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
                inputs = transaction.inputs.map { KaspaInput(
                    previousOutpoint = KaspaPreviousOutpoint(
                        transactionId = it.outpoint.hash.toString(),
                        index = it.outpoint.index
                    ),
                    signatureScript = it.scriptBytes.toHexString()
                ) },
                outputs = transaction.outputs.map { KaspaOutput(
                    amount = it.value.getValue(),
                    scriptPublicKey = KaspaScriptPublicKey(it.scriptBytes.toHexString())
                ) }
            )
        )
    }

    fun extractSignature(index: Int, signatures: ByteArray): ByteArray {
        val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s

        val canonicalSignature =
            Utils.bigIntegerToBytes(r, 32) + Utils.bigIntegerToBytes(canonicalS, 32)
        return canonicalSignature + SigHash.ALL.value.toByte()
    }

    private fun getTransaction() = transaction as KaspaTransaction

    private fun calculateChange(
        transactionData: TransactionData,
        unspentOutputs: List<KaspaUnspentOutput>
    ): BigDecimal {
        val fullAmount = unspentOutputs.map { it.amount }.reduce { acc, number -> acc + number }
        return fullAmount - (transactionData.amount.value!! + (transactionData.fee?.value
            ?: 0.toBigDecimal()))
    }

    fun getUnspentsCount() = unspentOutputs?.size ?: 0 // TODO: limit with 84?
}

internal fun TransactionData.toKaspaTransaction(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<KaspaUnspentOutput>,
    change: BigDecimal
): KaspaTransaction {

    val transaction = KaspaTransaction(
        networkParameters
    )
    transaction.setVersion(0)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
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
        KaspaAddressType.P2SH ->
            ScriptBuilder.createP2SHOutputScript(destinationAddressDecoded.hash) // TODO: throw exception
    }

    transaction.addOutput(
        Coin.parseCoin(this.amount.value!!.toPlainString()),
        destinationScript
    )
    if (!change.isZero()) {
        transaction.addOutput(
            Coin.parseCoin(change.toPlainString()),
            sourceScript
        )
    }
    return transaction
}