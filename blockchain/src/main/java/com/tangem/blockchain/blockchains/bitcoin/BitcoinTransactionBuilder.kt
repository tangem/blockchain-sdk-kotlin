package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.ducatus.DucatusMainNetParams
import com.tangem.blockchain.blockchains.litecoin.LitecoinMainNetParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.calculateRipemd160
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.isZero
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptPattern
import java.math.BigDecimal
import java.math.BigInteger

open class BitcoinTransactionBuilder(
        private val walletPublicKey: ByteArray, blockchain: Blockchain,
        walletAddresses: Set<com.tangem.blockchain.common.address.Address> = emptySet()
) {
    private val walletScripts =
            walletAddresses.filterIsInstance<BitcoinScriptAddress>().map { it.script }
    protected lateinit var transaction: Transaction

    protected var networkParameters = when (blockchain) {
        Blockchain.Bitcoin, Blockchain.BitcoinCash -> MainNetParams()
        Blockchain.BitcoinTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Ducatus -> DucatusMainNetParams()
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
    var unspentOutputs: List<BitcoinUnspentOutput>? = null

    open fun buildToSign(
            transactionData: TransactionData): Result<List<ByteArray>> {
        if (unspentOutputs == null) return Result.Failure(Exception("Currently there's an unconfirmed transaction"))

        val change: BigDecimal = calculateChange(transactionData, unspentOutputs!!)
        transaction = transactionData.toBitcoinJTransaction(networkParameters, unspentOutputs!!, change)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            val outputScript = Script(transaction.inputs[index].scriptBytes)
            val scriptToSign = when (outputScript.scriptType) {
                Script.ScriptType.P2PKH -> outputScript
                Script.ScriptType.P2SH -> findSpendingScript(outputScript)
                else -> throw Exception("Unsupported output script")
            }
            hashesForSign[index] = transaction.hashForSignature(
                    index,
                    scriptToSign,
                    Transaction.SigHash.ALL,
                    false
            ).bytes
        }
        return Result.Success(hashesForSign)
    }

    open fun buildToSend(signatures: ByteArray): ByteArray {
        for (index in transaction.inputs.indices) {
            val outputScript = Script(transaction.inputs[index].scriptBytes)
            val signature = extractSignature(index, signatures)

            transaction.inputs[index].scriptSig = when (outputScript.scriptType) {

                Script.ScriptType.P2PKH -> {
                    ScriptBuilder.createInputScript(signature, ECKey.fromPublicOnly(walletPublicKey))
                }
                Script.ScriptType.P2SH -> { // only 1 of 2 multisig script for now
                    val script = findSpendingScript(outputScript)
                    if (!ScriptPattern.isSentToMultisig(script)) {
                        throw Exception("Unsupported wallet script")
                    }
                    ScriptBuilder.createP2SHMultiSigInputScript(mutableListOf(signature), script)
                }
                else -> throw Exception("Unsupported output script")
            }
        }
        return transaction.bitcoinSerialize()
    }

    fun getTransactionHash() = transaction.txId.bytes

    fun getEstimateSize(transactionData: TransactionData): Result<Int> {
        val buildTransactionResult = buildToSign(transactionData)
        when (buildTransactionResult) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                val hashes = buildTransactionResult.data
                val finalTransaction = buildToSend(ByteArray(64 * hashes.size) { -128 }) // needed for longer signature
                return Result.Success(finalTransaction.size)
            }
        }
    }

    fun calculateChange(transactionData: TransactionData, unspentOutputs: List<BitcoinUnspentOutput>): BigDecimal {
        val fullAmount = unspentOutputs.map { it.amount }.reduce { acc, number -> acc + number }
        return fullAmount - (transactionData.amount.value!! + (transactionData.fee?.value
                ?: 0.toBigDecimal()))
    }

    open fun extractSignature(index: Int, signatures: ByteArray): TransactionSignature {
        val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        return TransactionSignature(r, canonicalS)
    }

    private fun findSpendingScript(outputScript: Script): Script {
        val scriptHash = ScriptPattern.extractHashFromP2SH(outputScript)
        return walletScripts.find {
            it.program.calculateSha256().calculateRipemd160().contentEquals(scriptHash)
        } ?: throw Exception("No script for P2SH output found")
    }
}

internal fun TransactionData.toBitcoinJTransaction(networkParameters: NetworkParameters?,
                                                   unspentOutputs: List<BitcoinUnspentOutput>,
                                                   change: BigDecimal): Transaction {
    val transaction = Transaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    transaction.addOutput(
            Coin.parseCoin(this.amount.value!!.toPlainString()),
            Address.fromString(networkParameters, this.destinationAddress))
    if (!change.isZero()) {
        transaction.addOutput(
                Coin.parseCoin(change.toPlainString()),
                Address.fromString(networkParameters,
                        this.sourceAddress))
    }
    return transaction
}

class BitcoinUnspentOutput(
        val amount: BigDecimal,
        val outputIndex: Long,
        val transactionHash: ByteArray,
        val outputScript: ByteArray
)