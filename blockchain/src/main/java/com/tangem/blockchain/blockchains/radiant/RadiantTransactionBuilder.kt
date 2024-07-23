package com.tangem.blockchain.blockchains.radiant

import com.tangem.blockchain.blockchains.radiant.models.RadiantAmountUnspentTransaction
import com.tangem.blockchain.blockchains.radiant.models.RadiantUnspentTransaction
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.bytes4LittleEndian
import com.tangem.blockchain.extensions.bytes8LittleEndian
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.electrum.ElectrumUnspentUTXORecord
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.Sha256Hash
import java.io.ByteArrayOutputStream

internal class RadiantTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
    private val decimals: Int,
) {

    private val dummySigner by lazy { DummySigner() }
    private val walletPublicKey = publicKey.blockchainKey.toCompressedPublicKey()
    private var utxo: List<ElectrumUnspentUTXORecord> = emptyList()

    fun setUnspentOutputs(unspents: List<ElectrumUnspentUTXORecord>) {
        utxo = unspents
    }

    fun buildForSign(transactionData: TransactionData): List<ByteArray> {
        transactionData.requireUncompiled()

        val outputScript = RadiantScriptUtils.buildOutputScript(address = transactionData.sourceAddress)
        val unspents = buildUnspents(listOf(outputScript))

        val txForPreimage = RadiantAmountUnspentTransaction(
            amount = transactionData.amount,
            fee = transactionData.fee,
            unspents = unspents,
        )

        val hashes = List(unspents.size) { index ->
            val preImageHash = buildPreImageHashes(
                tx = txForPreimage,
                targetAddress = transactionData.destinationAddress,
                sourceAddress = transactionData.sourceAddress,
                index = index,
            )

            Sha256Hash.hashTwice(preImageHash)
        }
        return hashes
    }

    fun buildForSend(transactionData: TransactionData, signatures: List<ByteArray>): ByteArray {
        transactionData.requireUncompiled()

        val outputScripts = RadiantScriptUtils.buildSignedScripts(signatures, walletPublicKey)
        val unspents = buildUnspents(outputScripts)
        val txForSigned = RadiantAmountUnspentTransaction(
            amount = transactionData.amount,
            fee = transactionData.fee,
            unspents = unspents,
        )

        return buildRawTransaction(
            tx = txForSigned,
            targetAddress = transactionData.destinationAddress,
            changeAddress = transactionData.sourceAddress,
        )
    }

    suspend fun estimateTransactionSize(transactionData: TransactionData): Int {
        val hashesForSign = buildForSign(transactionData = transactionData)
        val signatures =
            dummySigner.sign(hashesForSign, publicKey).successOr { throw BlockchainSdkError.FailedToLoadFee }
        val rawTx = buildForSend(transactionData = transactionData, signatures = signatures)
        return rawTx.size
    }

    @Suppress("MagicNumber")
    private fun buildPreImageHashes(
        tx: RadiantAmountUnspentTransaction,
        targetAddress: String,
        sourceAddress: String,
        index: Int,
    ): ByteArray {
        val txToSign = with(ByteArrayOutputStream()) {
            // version
            write(byteArrayOf(0x01, 0x00, 0x00, 0x00))

            // hashPrevouts (32-byte hash)
            write(RadiantScriptUtils.generatePrevoutHash(tx.unspents))

            // hashSequence (32-byte hash), ffffffff only
            write(RadiantScriptUtils.generateSequenceHash(tx.unspents))

            // outpoint (32-byte hash + 4-byte little endian)
            val currentOutput = tx.unspents.getOrNull(index) ?: throw BlockchainSdkError.FailedToBuildTx
            write(currentOutput.hash.reversedArray())
            write(currentOutput.outputIndex.toInt().bytes4LittleEndian())

            // scriptCode of the input (serialized as scripts inside CTxOuts)
            val scriptCode = RadiantScriptUtils.buildOutputScript(address = sourceAddress)
            write(scriptCode.count())
            write(scriptCode)

            // value of the output spent by this input (8-byte little endian)
            write(currentOutput.amount.bytes8LittleEndian())

            // nSequence of the input (4-byte little endian), ffffffff only
            write(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))

            // hashOutputHashes (32-byte hash)
            write(
                RadiantScriptUtils.writeHashOutputHashes(
                    amount = tx.amountSatoshiDecimalValue,
                    sourceAddress = sourceAddress,
                    targetAddress = targetAddress,
                    change = tx.changeSatoshiDecimalValue,
                ),
            )

            // hashOutputs (32-byte hash)
            write(
                RadiantScriptUtils.writeHashOutput(
                    amount = tx.amountSatoshiDecimalValue,
                    sourceAddress = sourceAddress,
                    targetAddress = targetAddress,
                    change = tx.changeSatoshiDecimalValue,
                ),
            )

            // nLocktime of the transaction (4-byte little endian)
            write(byteArrayOf(0x00, 0x00, 0x00, 0x00))

            // sighash type of the signature (4-byte little endian)
            write(byteArrayOf(0x41, 0x00, 0x00, 0x00))

            toByteArray()
        }
        return txToSign
    }

    private fun buildRawTransaction(
        tx: RadiantAmountUnspentTransaction,
        targetAddress: String,
        changeAddress: String,
    ): ByteArray {
        val txBody = with(ByteArrayOutputStream()) {
            // version
            write(byteArrayOf(0x01, 0x00, 0x00, 0x00))

            // 01
            write(tx.unspents.count())

            // hex str hash prev btc
            tx.unspents.forEach { input ->
                val hashKey = input.hash.reversedArray()
                write(hashKey)
                write(input.outputIndex.toInt().bytes4LittleEndian())

                write(input.outputScript.count())
                write(input.outputScript)

                // ffffffff
                write(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
            }

            // 02
            val outputCount = if (tx.changeSatoshiDecimalValue == 0L) 1 else 2
            write(outputCount)

            // 8 bytes
            write(tx.amountSatoshiDecimalValue.bytes8LittleEndian())

            val outputScriptBytes = RadiantScriptUtils.buildOutputScript(address = targetAddress)

            // hex str 1976a914....88ac
            write(outputScriptBytes.count())
            write(outputScriptBytes)

            if (tx.changeSatoshiDecimalValue != 0L) {
                // 8 bytes of change satoshi value
                write(tx.changeSatoshiDecimalValue.bytes8LittleEndian())

                val outputScriptChangeBytes = RadiantScriptUtils.buildOutputScript(address = changeAddress)
                write(outputScriptChangeBytes.count())
                write(outputScriptChangeBytes)
            }

            // 00000000
            write(byteArrayOf(0x00, 0x00, 0x00, 0x00))

            toByteArray()
        }
        return txBody
    }

    private fun buildUnspents(outputScripts: List<ByteArray>): List<RadiantUnspentTransaction> {
        return utxo.mapIndexed { index, txRef ->
            val outputScript = if (outputScripts.count() == 1) outputScripts.first() else outputScripts.getOrNull(index)
            outputScript ?: throw BlockchainSdkError.FailedToBuildTx
            RadiantUnspentTransaction(
                amount = txRef.value.movePointRight(decimals).toLong(),
                outputIndex = txRef.txPos,
                hash = txRef.txHash.hexToBytes(),
                outputScript = outputScript,
            )
        }
    }
}