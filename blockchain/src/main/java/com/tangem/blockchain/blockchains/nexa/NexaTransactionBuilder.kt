package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import com.tangem.blockchain.blockchains.nexa.models.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.getMinimumRequiredUTXOsToSendSatoshi
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.calculateSha256
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptChunk
import org.bitcoinj.script.ScriptOpCodes
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class NexaTransactionBuilder(
    private val walletPublicKey: ByteArray,
    private val blockchain: Blockchain,
) {
    private lateinit var transaction: NexaTransaction
    private var networkParameters = NexaMainNetParams()
    private val addressService = NexaAddressService(blockchain.isTestnet())
    var unspentOutputs: List<NexaUnspentOutput>? = null

    var currentBlockTipHeight: Long = MAX_LOCK_TIME_BLOCK_HEIGHT // TODO make immutable argument

    private var transactionSizeWithoutWitness = 0

    fun buildToSign(transactionData: TransactionData): Result<NexaTransactionNative> {

        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(BlockchainSdkError.CustomError("Unspent outputs are missing"))
        }
        if (currentBlockTipHeight >= MAX_LOCK_TIME_BLOCK_HEIGHT) {
            return Result.Failure(BlockchainSdkError.CustomError("The block height is higher than allowed"))
        }

        val amount = transactionData.amount.value!!.setScale(Blockchain.Nexa.decimals(), RoundingMode.HALF_UP)
        val fee = (transactionData.fee?.amount?.value ?: BigDecimal.ZERO)
            .setScale(
                Blockchain.Nexa.decimals(),
                RoundingMode.HALF_UP
            )

        val satoshiAmount = amount
            .movePointRight(Blockchain.Nexa.decimals())
            .longValueExact()

        if (satoshiAmount <= DUST_SATOSHI_AMOUNT) {
            return Result.Failure(BlockchainSdkError.CustomError("Sending dust is not allowed"))
        }

        val satoshiFee = fee
            .movePointRight(Blockchain.Nexa.decimals())
            .longValueExact()

        if (satoshiFee > MAX_FEE_SATOSHI) {
            return Result.Failure(BlockchainSdkError.CustomError("Sending transaction with fee higher than allowed"))
        }

        val outputsToSend = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentOutputs!!,
            transactionSatoshiAmount = satoshiAmount,
            transactionSatoshiFeeAmount = satoshiFee,
            unspentToSatoshiAmount = { it.amountSatoshi }
        )

        if (outputsToSend.size > MAX_INPUT_COUNT) {
            return Result.Failure(BlockchainSdkError.CustomError("Max input count"))
        }

        val change = calculateChangeSatoshi(
            amount = satoshiAmount,
            fee = satoshiFee,
            unspentOutputs = outputsToSend
        )

        val nexaTransaction = transactionData.toNexaTransaction(
            unspentOutputs = unspentOutputs!!,
            changeSatoshi = change,
            blockchain = blockchain,
            lockTime = currentBlockTipHeight
        )

        val inputsHashes = nexaTransaction.inputs.map {
            val subscript = when (it.addressType) {
                NexaAddressType.P2PKH -> {
                    it.output.script
                }
                NexaAddressType.TEMPLATE -> {
                    ScriptBuilder()
                        .addChunk(ScriptChunk(ScriptOpCodes.OP_FROMALTSTACK, null))
                        .addChunk(ScriptChunk(ScriptOpCodes.OP_CHECKSIGVERIFY, null))
                        .build()
                }
                else -> error("Not supported")
            }

            sighashForNexa(nexaTransaction, subscript)
        }

        val nexaTransactionForSign = nexaTransaction.prepareForSign(inputsHashes)


        return Result.Success(nexaTransactionForSign)
    }

    // Output.prototype.toBufferWriter = function(writer) {
    //     if (!writer) {
    //         writer = new BufferWriter();
    //     }
    //     writer.writeUInt8(this.type);
    //     writer.writeUInt64LEBN(this._satoshisBN);
    //     var script = this._scriptBuffer;
    //     writer.writeVarintNum(script.length);
    //     writer.write(script);
    //     return writer;

    fun sighashForNexa(transaction: NexaTransactionNative, inputSubscript: Script): ByteArray {
        fun getPreviousHash(): ByteArray {
            val res = UnsafeByteArrayOutputStream()
            transaction.inputs.forEach {
                res.write(it.type)
                res.write(it.prevTxId.reversedArray())
            }

            return res.toByteArray().calculateSha256().calculateSha256()
        }

        fun getSequenceHash(): ByteArray {
            val res = UnsafeByteArrayOutputStream()
            transaction.inputs.forEach {
                Utils.uint32ToByteStreamLE(it.sequenceNumber.toLong(), res) //TODO check if is it 4278190079 fe ff ff ff
            }
            return res.toByteArray().calculateSha256().calculateSha256()
        }

        fun getInputAmountHash(): ByteArray {
            val res = UnsafeByteArrayOutputStream()
            transaction.inputs.forEach {
                Utils.uint64ToByteStreamLE(it.amountSatoshi.toBigInteger(), res)
            }
            return res.toByteArray().calculateSha256().calculateSha256()
        }

        fun getOutputsHash(): ByteArray {
            val res = UnsafeByteArrayOutputStream()
            transaction.outputs.forEach {
                res.write(it.serialize())
            }
            return res.toByteArray().calculateSha256().calculateSha256()
        }

        val previousHash = getPreviousHash()
        val hashSequence = getSequenceHash()
        val hashInputAmount = getInputAmountHash()
        val hashOutputs = getOutputsHash()

        val bytes = UnsafeByteArrayOutputStream()

        //version
        bytes.write(transaction.version)

        bytes.write(previousHash)
        bytes.write(hashInputAmount)
        bytes.write(hashSequence)

        val subscriptBytes = inputSubscript.program
        bytes.write(VarInt(subscriptBytes.size.toLong()).encode())
        bytes.write(subscriptBytes)

        bytes.write(hashOutputs)

        Utils.uint32ToByteStreamLE(transaction.lockTime, bytes)
        bytes.write(0x00) //Signature.SIGHASH_NEXA_ALL

        return bytes.toByteArray().calculateSha256().calculateSha256().reversedArray()
    }

    fun buildToSend(signedTransaction: NexaTransactionNative): ByteArray {
        return signedTransaction.serialize()
    }

    fun getEstimateSize(transactionData: TransactionData): Result<Int> {


        val txToSign = buildToSign(transactionData).successOr { return Result.Failure(it.error) }

        val dummySignatures = List(txToSign.inputs.size) { ByteArray(64) { -128 } }

        val signedTransaction = txToSign.sign(dummySignatures, walletPublicKey)

        val transactionHash = buildToSend(signedTransaction)

        return Result.Success(transactionHash.size)
    }

    /**
     * Transaction.prototype.getSignatures = function(privKey, sigtype, signingMethod) {
     *   privKey = new PrivateKey(privKey);
     *
     *   var transaction = this;
     *   var results = [];
     *
     *   var scriptPushPubKey = Script.empty().add(privKey.publicKey.toBuffer());
     *   var hashData = Hash.sha256ripemd160(scriptPushPubKey.toBuffer());
     *
     *   for (let index = 0; index < this.inputs.length; index++) {
     *     var input = this.inputs[index];
     *     var signatures = input.getSignatures(transaction, privKey, index, sigtype, hashData, signingMethod);
     *     for (let signature of signatures) {
     *       results.push(signature);
     *     }
     *   }
     *   return results;
     * };
     */

    // @Suppress("MagicNumber")
    // private fun extractSignature(index: Int, signatures: ByteArray): ByteArray {
    //     val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
    //     val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
    //     val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
    //
    //     val canonicalSignature =
    //         Utils.bigIntegerToBytes(r, 32) + Utils.bigIntegerToBytes(canonicalS, 32)
    //     return canonicalSignature + Transaction.SigHash.ALL.value.toByte()
    // }

    @Suppress("MagicNumber")
    private fun extractSignature(index: Int, signatures: ByteArray): TransactionSignature {
        val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        return TransactionSignature(r, canonicalS) //TODO check bitcoincash?
    }

    private fun calculateChangeSatoshi(amount: Long, fee: Long, unspentOutputs: List<NexaUnspentOutput>): Long {
        val fullAmount = unspentOutputs.map { it.amountSatoshi }.reduce { acc, number -> acc + number }
        val change = fullAmount - (amount + fee)
        return change.takeIf { it > DUST_SATOSHI_AMOUNT } ?: 0L
    }

    companion object {
        const val DUST_SATOSHI_AMOUNT = 546
        const val MAX_INPUT_COUNT = 250
        const val MAX_LOCK_TIME_BLOCK_HEIGHT = 500_000_000L
        const val MAX_FEE_SATOSHI = 20000
    }
}

// override fun send(
//     amountSatoshis: Long, destScript: SatoshiScript, deductFeeFromAmount: Boolean = 0,
//     sync: Boolean =
//         false,
//     note:
//     String? = "",
// ): iTransaction {
//     val (signedTx, serializedTx) = try  // Try at least 1 confirmation first
//     {
//         txConstructor(amountSatoshis, deductFeeFromAmount, 1) { tx, fee ->
//             // Add the output that we are sending to
//             val output = txOutputFor(chainSelector)
//             output.amount = if (deductFeeFromAmount) amountSatoshis - fee else amountSatoshis
//             output.script = destScript
//             tx.add(output)
//             deductFeeFromAmount
//         }
//     } catch (e: WalletNotEnoughBalanceException)  // Try unconfirmed
//     {
//         txConstructor(amountSatoshis, deductFeeFromAmount, 0) { tx, fee ->
//             // Add the output that we are sending to
//             val output = txOutputFor(chainSelector)
//             output.amount = if (deductFeeFromAmount) amountSatoshis - fee else amountSatoshis
//             output.script = destScript
//             tx.add(output)
//             deductFeeFromAmount
//         }
//     }
//
//     LogIt.info(sourceLoc() + " " + name + ": Sending TX " + signedTx.idem.toHex())
//     LogIt.info(sourceLoc() + " " + name + ": TX hex " + serializedTx.toHex())
//     if (sync)
//         commitWalletTransaction(signedTx, serializedTx, note)
//     else launch { commitWalletTransaction(signedTx, serializedTx, note) }
//     return signedTx
// }

internal fun TransactionData.toNexaTransaction(
    unspentOutputs: List<NexaUnspentOutput>,
    changeSatoshi: Long,
    blockchain: Blockchain,
    lockTime: Long,
): NexaTransactionNative {
    val addressService = NexaAddressService(isTestNet = blockchain.isTestnet())
    val payDestinationSatoshiAmount = amount.value!!.movePointRight(blockchain.decimals()).longValueExact()

    val sourceAddressParts = addressService.getAddressDecodedParts(sourceAddress)
    val destinationAddressParts = addressService.getAddressDecodedParts(destinationAddress)

    val inputs = unspentOutputs.map { utxo ->
        NexaTxInputNative(
            output = NexaTxInputNative.Output(
                script = sourceAddressParts.outputScript(),
                valueSatoshi = utxo.amountSatoshi,
            ),
            prevTxId = utxo.outpointHash,
            outputIndex = utxo.outputIndex,
            hashToSign = NexaTxInputNative.SignHash.Empty,
            amountSatoshi = utxo.amountSatoshi,
            addressType = sourceAddressParts.addressType,
            sequenceNumber = Int.MAX_VALUE //TODO check if is it uint32
        )
    }

    val mainOutput = NexaTxOutput(
        satoshiAmount = payDestinationSatoshiAmount,
        script = destinationAddressParts.outputScript(),
        type = destinationAddressParts.outputType(),
    )

    val changeOutput = if (changeSatoshi != 0L) {
        NexaTxOutput(
            satoshiAmount = changeSatoshi,
            script = sourceAddressParts.outputScript(),
            type = sourceAddressParts.outputType()
        )
    } else {
        null
    }

    // transaction.addOutput(
    //     Coin.valueOf(payDestinationSatoshiAmount),
    //     destinationScript
    // )
    // if (changeSatoshi != 0L) {
    //     transaction.addOutput(
    //         Coin.valueOf(changeSatoshi),
    //         sourceAddressParts.outputScript()
    //     )
    // }

    return NexaTransactionNative(
        inputs = inputs,
        outputs = listOfNotNull(mainOutput, changeOutput),
        lockTime = lockTime
    )
}

internal fun NexaTxOutput.serialize(): ByteArray {
    val res = UnsafeByteArrayOutputStream()
    res.write(type.byte.toInt())
    Utils.uint64ToByteStreamLE(satoshiAmount.toBigInteger(), res)
    val script = script.program
    res.write(VarInt(script.size.toLong()).encode())
    res.write(script)
    return res.toByteArray()
}

internal fun NexaTransactionNative.serialize(): ByteArray {
    val transaction = this
    val bytes = UnsafeByteArrayOutputStream()

    val version = 0x0
    bytes.write(version) //1

    val inputsCount = transaction.inputs.size // always < MAX_INPUT_COUNT
    bytes.write(VarInt(inputsCount.toLong()).encode())
    // writer.writeUInt8(this.type);
    // writer.writeReverse(this.prevTxId);
    //
    // var script = this._scriptBuffer;
    // writer.writeVarintNum(script.length);
    // writer.write(script);
    // writer.writeUInt32LE(this.sequenceNumber);
    // writer.writeUInt64LEBN(this.amountBN);
    // return writer;

    transaction.inputs.forEach {
        bytes.write(it.addressType.versionByte.toInt())
        bytes.write(it.prevTxId.reversedArray())

        // val script = it.script.program
        // bytes.write(VarInt(script.size.toLong()).encode())
        // bytes.write(script)
        val scriptBytes =
            (it.hashToSign as? NexaTxInputNative.SignHash.Signed ?: error("transaction is not signed")).hash
        bytes.write(VarInt(scriptBytes.size.toLong()).encode())
        bytes.write(scriptBytes)
        Utils.uint32ToByteStreamLE(it.sequenceNumber.toLong(), bytes)
        Utils.uint64ToByteStreamLE(it.amountSatoshi.toBigInteger(), bytes)
    }

    val outputCount = transaction.outputs.size
    bytes.write(VarInt(outputCount.toLong()).encode())

    // writer.writeUInt8(this.type);
    // writer.writeUInt64LEBN(this._satoshisBN);
    // var script = this._scriptBuffer;
    // writer.writeVarintNum(script.length);
    // writer.write(script);

    transaction.outputs.forEach {
        bytes.write(it.serialize())
        // bytes.write(it.type.byte.toInt())
        // Utils.uint64ToByteStreamLE(it.satoshiAmount.toBigInteger(), bytes)
        // val script = it.script.program
        // bytes.write(VarInt(script.size.toLong()).encode())
        // bytes.write(script)
    }

    val lockTime = transaction.lockTime
    Utils.uint32ToByteStreamLE(lockTime, bytes)

    return bytes.toByteArray()
}