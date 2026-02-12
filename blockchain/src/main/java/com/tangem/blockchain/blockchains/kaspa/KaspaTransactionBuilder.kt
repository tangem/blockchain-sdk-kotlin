package com.tangem.blockchain.blockchains.kaspa

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaAddressType
import com.tangem.blockchain.blockchains.kaspa.kaspacashaddr.KaspaCashAddr
import com.tangem.blockchain.blockchains.kaspa.krc20.model.*
import com.tangem.blockchain.blockchains.kaspa.network.*
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.orZero
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.*
import org.bitcoinj.core.Transaction.SigHash
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes.*
import org.bouncycastle.jcajce.provider.digest.Blake2b
import java.math.BigDecimal
import java.math.BigInteger

class KaspaTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
    private val isTestnet: Boolean,
) {
    private var networkParameters = KaspaMainNetParams()
    var unspentOutputs: List<KaspaUnspentOutput>? = null

    private val addressService = KaspaAddressService(isTestnet)

    @OptIn(ExperimentalStdlibApi::class)
    private val envelopeAdapter by lazy { moshi.adapter<Envelope>() }

    @Suppress("MagicNumber")
    fun buildToSign(transactionData: TransactionData, dustValue: BigDecimal): Result<KaspaTransaction> {
        val uncompiledTransaction = transactionData.requireUncompiled()

        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Unspent outputs are missing"),
            )
        }

        val unspentsToSpend = getUnspentsToSpend()

        val change = calculateChange(
            amount = requireNotNull(uncompiledTransaction.amount.value) { "Transaction amount is null" },
            fee = uncompiledTransaction.fee?.amount?.value ?: BigDecimal.ZERO,
            dustValue = dustValue,
            unspentOutputs = unspentsToSpend,
        )
        if (change < BigDecimal.ZERO) { // unspentsToSpend not enough to cover transaction amount
            val maxAmount = uncompiledTransaction.amount.value + change
            return Result.Failure(BlockchainSdkError.Kaspa.UtxoAmountError(MAX_INPUT_COUNT, maxAmount))
        }

        val addressService = KaspaAddressService(isTestnet)
        val sourceScript = ScriptBuilder().data(addressService.getPublicKey(uncompiledTransaction.sourceAddress)).op(
            OP_CODESEPARATOR,
        ).build()

        val destinationAddressDecoded =
            KaspaCashAddr(isTestnet).decodeCashAddress(uncompiledTransaction.destinationAddress)
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

        val transaction = createKaspaTransaction(
            networkParameters = networkParameters,
            unspentOutputs = unspentsToSpend,
            transformer = { kaspaTransaction ->
                kaspaTransaction.addOutput(
                    Coin.parseCoin(uncompiledTransaction.amount.value.toPlainString()),
                    destinationScript,
                )
                if (!change.isZero()) {
                    kaspaTransaction.addOutput(
                        Coin.parseCoin(change.toPlainString()),
                        sourceScript,
                    )
                }
                kaspaTransaction
            },
        )

        return Result.Success(transaction)
    }

    fun buildToSend(signatures: ByteArray, transaction: KaspaTransaction): KaspaTransactionBody {
        for (index in transaction.inputs.indices) {
            val signature = extractSignature(index, signatures)
            transaction.inputs[index].scriptSig = ScriptBuilder().data(signature).build()
        }
        return buildForSendInternal(transaction)
    }

    internal fun buildToSendKRC20Reveal(
        signatures: ByteArray,
        redeemScript: RedeemScript,
        transaction: KaspaTransaction,
    ): KaspaTransactionBody {
        for (index in transaction.inputs.indices) {
            val signature = extractSignature(index, signatures)
            if (index == 0) {
                transaction.inputs[index].scriptSig = ScriptBuilder()
                    .data(signature)
                    .data(redeemScript.script().program)
                    .build()
            } else {
                transaction.inputs[index].scriptSig = ScriptBuilder().data(signature).build()
            }
        }
        return buildForSendInternal(transaction)
    }

    @Suppress("LongMethod", "MagicNumber")
    internal fun buildToSignKRC20Commit(
        transactionData: TransactionData,
        dustValue: BigDecimal,
        includeFee: Boolean = true,
    ): Result<CommitTransaction> {
        val uncompiledTransaction = transactionData.requireUncompiled()

        require(uncompiledTransaction.amount.type is AmountType.Token)

        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Unspent outputs are missing"),
            )
        }

        val unspentsToSpend = getUnspentsToSpend()

        val transactionFeeAmountValue = uncompiledTransaction.fee?.amount?.value ?: BigDecimal.ZERO

        val revealFeeAmount = (uncompiledTransaction.fee as? Fee.Kaspa)
            ?.revealTransactionFee
            ?.takeIf { includeFee }
            ?.value
            ?: BigDecimal.ZERO

        val commitFeeAmount = if (includeFee) {
            transactionFeeAmountValue - revealFeeAmount
        } else {
            transactionFeeAmountValue
        }

        val targetOutputAmountValue = revealFeeAmount + dustValue

        val resultChange = calculateChange(
            amount = targetOutputAmountValue,
            fee = commitFeeAmount,
            dustValue = dustValue,
            unspentOutputs = getUnspentsToSpend(),
        )

        val envelope = Envelope(
            p = "krc-20",
            op = "transfer",
            amt = uncompiledTransaction.amount.longValue.toString(),
            to = uncompiledTransaction.destinationAddress,
            tick = uncompiledTransaction.amount.type.token.contractAddress,
        )

        val redeemScript = RedeemScript(
            publicKey = publicKey.blockchainKey.toCompressedPublicKey(),
            envelope = envelope,
        )

        val transaction = createKaspaTransaction(
            networkParameters = networkParameters,
            unspentOutputs = unspentsToSpend,
            transformer = { kaspaTransaction ->
                kaspaTransaction.addOutput(
                    Coin.parseCoin(targetOutputAmountValue.toPlainString()),
                    redeemScript.scriptHash(),
                )
                if (!resultChange.isZero()) {
                    val addressService = KaspaAddressService(isTestnet)
                    val sourceScript = ScriptBuilder()
                        .data(addressService.getPublicKey(uncompiledTransaction.sourceAddress))
                        .op(OP_CODESEPARATOR)
                        .build()
                    kaspaTransaction.addOutput(
                        Coin.parseCoin(resultChange.toPlainString()),
                        sourceScript,
                    )
                }
                kaspaTransaction
            },
        )
        val commitTransaction = CommitTransaction(
            transaction = transaction,
            hashes = getHashesForSign(transaction),
            redeemScript = redeemScript,
            sourceAddress = uncompiledTransaction.sourceAddress,
            params = BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction(
                transactionId = transaction.transactionHash().toHexString(),
                amountValue = uncompiledTransaction.amount.value ?: BigDecimal.ZERO,
                feeAmountValue = targetOutputAmountValue,
                envelope = envelope,
            ),
        )

        return Result.Success(commitTransaction)
    }

    internal fun buildToSignKRC20Reveal(
        sourceAddress: String,
        redeemScript: RedeemScript,
        params: BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction,
        revealFeeAmountValue: BigDecimal,
        dustValue: BigDecimal,
    ): Result<RevealTransaction> {
        val utxo = listOf(
            KaspaUnspentOutput(
                amount = params.feeAmountValue,
                outputIndex = 0,
                transactionHash = params.transactionId.hexToBytes(),
                outputScript = redeemScript.scriptHash().program,
            ),
        )

        val change = calculateChange(
            amount = BigDecimal.ZERO,
            fee = revealFeeAmountValue,
            dustValue = dustValue.orZero(),
            unspentOutputs = utxo,
        )

        val transaction = createKaspaTransaction(
            networkParameters = networkParameters,
            unspentOutputs = utxo,
            transformer = { kaspaTransaction ->
                val sourceScript = ScriptBuilder()
                    .data(addressService.getPublicKey(sourceAddress))
                    .op(OP_CODESEPARATOR)
                    .build()
                kaspaTransaction.addOutput(
                    Coin.parseCoin(change.toPlainString()),
                    sourceScript,
                )
                kaspaTransaction
            },
        )

        val revealTransaction = RevealTransaction(
            transaction = transaction,
            hashes = getHashesForSign(transaction),
            redeemScript = redeemScript,
        )

        return Result.Success(revealTransaction)
    }

    private fun buildForSendInternal(transaction: KaspaTransaction) = KaspaTransactionBody(
        KaspaTransactionData(
            inputs = transaction.inputs.map {
                it.scriptSig.program
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

    @Suppress("MagicNumber")
    private fun extractSignature(index: Int, signatures: ByteArray): ByteArray {
        val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s

        val canonicalSignature =
            Utils.bigIntegerToBytes(r, 32) + Utils.bigIntegerToBytes(canonicalS, 32)
        return canonicalSignature + SigHash.ALL.value.toByte()
    }

    private fun calculateChange(
        amount: BigDecimal,
        fee: BigDecimal,
        dustValue: BigDecimal,
        unspentOutputs: List<KaspaUnspentOutput>,
    ): BigDecimal {
        val fullAmount = unspentOutputs.sumOf { it.amount }
        val change = fullAmount - (amount + fee)

        return if (change > BigDecimal.ZERO && change < dustValue) {
            throw BlockchainSdkError.TransactionDustChangeError
        } else {
            change
        }
    }

    fun getUnspentsToSpendCount(): Int {
        val count = unspentOutputs?.size ?: 0
        return if (count < MAX_INPUT_COUNT) count else MAX_INPUT_COUNT
    }

    fun getUnspentsToSpend() = unspentOutputs!!.sortedByDescending { it.amount }.take(getUnspentsToSpendCount())

    fun getUnspentsToSpendAmount() = getUnspentsToSpend().sumOf { it.amount }

    fun getHashesForSign(transaction: KaspaTransaction): List<ByteArray> {
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
        return hashesForSign
    }

    private fun RedeemScript.script(): Script {
        val kasplexId = "kasplex".toByteArray()
        val payload = envelopeAdapter.toJson(envelope).toByteArray()

        return ScriptBuilder()
            .data(publicKey)
            .op(OP_CODESEPARATOR)
            .opFalse()
            .op(OP_IF)
            .data(kasplexId)
            .opTrue()
            .opFalse()
            .opFalse()
            .data(payload)
            .op(OP_ENDIF)
            .build()
    }

    private fun RedeemScript.scriptHash(): Script = ScriptBuilder()
        .op(OP_HASH256)
        .data(Blake2b.Blake2b256().digest(script().program))
        .op(OP_EQUAL)
        .build()

    companion object {
        const val MAX_INPUT_COUNT = 84 // Kaspa rejects transactions with more inputs
    }
}

internal fun createKaspaTransaction(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<KaspaUnspentOutput>,
    transformer: (KaspaTransaction) -> KaspaTransaction,
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

    return transformer(transaction)
}