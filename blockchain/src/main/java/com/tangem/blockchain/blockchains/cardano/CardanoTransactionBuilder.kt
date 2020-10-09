package com.tangem.blockchain.blockchains.cardano

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.DataItem
import com.tangem.blockchain.blockchains.cardano.crypto.Blake2b
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

class CardanoTransactionBuilder(private val walletPublicKey: ByteArray) {

    var unspentOutputs: List<UnspentOutput> = listOf()
    private var transactionBody: DataItem? = null
    private var useShelleyWitness: Boolean? = null

    fun buildToSign(transactionData: TransactionData): ByteArray {
        useShelleyWitness = CardanoAddressService.isShelleyAddress(transactionData.sourceAddress)

        val transactionMap = CborBuilder().addMap()

        transactionMap.put(0.toDataItem(), createInputsDataItem())
        transactionMap.put(1.toDataItem(), createOutputsDataItem(transactionData))
        transactionMap.put(2, transactionData.fee!!.longValue!!)
        transactionMap.put(3, 90000000) // ttl

        val unsignedTransaction = transactionMap.end().build()
        transactionBody = unsignedTransaction[0]

        val baos = ByteArrayOutputStream()
        CborEncoder(baos).encode(unsignedTransaction)

        return Blake2b.Digest.newInstance(32).digest(baos.toByteArray())
    }

    fun buildToSend(signature: ByteArray): ByteArray {
        val txArray = CborBuilder().addArray()
        txArray.add(transactionBody)
        val witnessMap = txArray.addMap()
        txArray.add(null as DataItem?)

        if (useShelleyWitness!!) {
            witnessMap.put(0.toDataItem(), createShelleyWitnessDataItem(signature))
        } else {
            witnessMap.put(2.toDataItem(), createByronWitnessDataItem(signature))
        }

        val baos = ByteArrayOutputStream()
        CborEncoder(baos).encode(txArray.end().build())
        return baos.toByteArray()
    }

    private fun Int.toDataItem(): DataItem? {
        return CborBuilder().add(this.toLong()).build()[0]
    }

    private fun createInputsDataItem(): DataItem? {
        val inputsArray = CborBuilder().addArray()

        unspentOutputs.forEach {
            inputsArray.addArray()
                    .add(it.transactionHash)
                    .add(it.outputIndex)
        }

        return inputsArray.end().build()[0]
    }

    private fun createOutputsDataItem(transactionData: TransactionData): DataItem? {
        val amount = transactionData.amount.longValue!!
        val change = calculateChange(amount, transactionData.fee!!.longValue!!)

        val sourceBytes = CardanoAddressService.decode(transactionData.sourceAddress)
        val destinationBytes = CardanoAddressService.decode(transactionData.destinationAddress)

        val outputsArray = CborBuilder().addArray()

        outputsArray
                .addArray()
                .add(destinationBytes)
                .add(amount)

        if (change > 0) {
            outputsArray
                    .addArray()
                    .add(sourceBytes)
                    .add(change)
                    .end()
        }
        return outputsArray.end().build()[0]
    }

    private fun createByronWitnessDataItem(signature: ByteArray): DataItem? {
        return CborBuilder().addArray().addArray()
                .add(walletPublicKey)
                .add(signature)
                .add(ByteArray(32))
                .add("A0".hexToBytes())
                .end().end().build().get(0)
    }

    private fun createShelleyWitnessDataItem(signature: ByteArray): DataItem? {
        return CborBuilder().addArray().addArray()
                .add(walletPublicKey)
                .add(signature)
                .end().end().build().get(0)
    }

    private fun calculateChange(amount: Long, fee: Long): Long {
        val fullAmount = unspentOutputs.map { it.amount }.sum()
        return fullAmount - (amount + fee)
    }

//    fun getEstimateSize(transactionData: TransactionData): Int {
//        val fullAmount = unspentOutputs.map { it.amount }.sum()
//        val outputsNumber = if (transactionData.amount.longValue == fullAmount) 1 else 2
//        return unspentOutputs.size * 40 + outputsNumber * 65 + 160
//    }

    fun getEstimateSize(transactionData: TransactionData): Int {
        val dummyFeeValue = BigDecimal.valueOf(0.1)

        val dummyFee = transactionData.amount.copy(value = dummyFeeValue)
        val dummyAmount =
                transactionData.amount.copy(value = transactionData.amount.value!! - dummyFeeValue)

        val dummyTransactionData = transactionData.copy(
                amount = dummyAmount,
                fee = dummyFee
        )
        buildToSign(dummyTransactionData)
        return buildToSend(ByteArray(64)).size
    }
}

class UnspentOutput(
        val amount: Long,
        val outputIndex: Long,
        val transactionHash: ByteArray
)