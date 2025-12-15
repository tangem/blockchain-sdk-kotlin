package com.tangem.blockchain.blockchains.ton

import com.google.protobuf.ByteString
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ton.models.TonCompiledTransactionData
import com.tangem.blockchain.blockchains.ton.models.TonPreSignStructure
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.bytes8BigEndian
import com.tangem.blockchain.extensions.removeLeadingZeros
import com.tangem.blockchain.network.moshi
import org.ton.block.CommonMsgInfoRelaxed
import org.ton.block.Either
import org.ton.block.MessageRelaxed
import org.ton.block.MsgAddressInt
import org.ton.boc.BagOfCells
import org.ton.cell.Cell
import wallet.core.jni.Base64
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TheOpenNetwork
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

internal class TonTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
    private val walletAddress: String,
) {

    @OptIn(ExperimentalStdlibApi::class)
    private val compiledTransactionAdapter by lazy { moshi.adapter<TonCompiledTransactionData>() }
    private val messageRelaxedCodec = MessageRelaxed.tlbCodec(CellTlbConstructorFixed)
    private val coinType = CoinType.TON
    private var jettonWalletAddresses: Map<Token, String> = emptyMap()
    private val modeTransactionConstant: Int =
        TheOpenNetwork.SendMode.PAY_FEES_SEPARATELY_VALUE.or(TheOpenNetwork.SendMode.IGNORE_ACTION_PHASE_ERRORS_VALUE)

    fun updateJettonAdresses(fetchedJettonWalletAddresses: Map<Token, String>) {
        jettonWalletAddresses = fetchedJettonWalletAddresses
    }

    fun buildForSign(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        expireAt: Int,
        extras: TonTransactionExtras? = null,
    ): TonPreSignStructure {
        val input = buildInput(
            sequenceNumber = sequenceNumber,
            amount = amount,
            destination = destination,
            comment = extras?.memo.orEmpty(),
            expireAt = expireAt,
        )
        val inputData = input.toByteArray()
        val preImageHashes = TransactionCompiler.preImageHashes(coinType, inputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return TonPreSignStructure(
            hashToSign = preSigningOutput.data.toByteArray(),
            inputData = inputData,
        )
    }

    fun buildCompiledForSign(transactionData: TransactionData.Compiled, expireAt: Int): TonPreSignStructure {
        val compiledTransaction = (transactionData.value as? TransactionData.Compiled.Data.RawString)?.data
            ?: error("Compiled transaction must be in hex format")
        val parsed = compiledTransactionAdapter.fromJson(compiledTransaction)
            ?: error("Unable to parse compiled transaction")
        val messageRelaxed = parseMessage(parsed)
        val info = messageRelaxed.info as CommonMsgInfoRelaxed.IntMsgInfoRelaxed
        val transfer = TheOpenNetwork.Transfer.newBuilder()
            .setDest(MsgAddressInt.toString(address = info.dest))
            .setAmount(ByteString.copyFrom(info.value.coins.amount.value.toLong().bytes8BigEndian()))
            .setMode(modeTransactionConstant)
            .setComment(messageRelaxed.extractComment())
            .setBounceable(info.bounce)
            .build()
        val input = TheOpenNetwork.SigningInput
            .newBuilder()
            .addMessages(transfer)
            .setWalletVersion(TheOpenNetwork.WalletVersion.WALLET_V4_R2)
            .setSequenceNumber(parsed.seqno)
            .setExpireAt(expireAt)
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey))
            .build()

        val inputData = input.toByteArray()
        val preImageHashes = TransactionCompiler.preImageHashes(coinType, inputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return TonPreSignStructure(
            hashToSign = preSigningOutput.data.toByteArray(),
            inputData = inputData,
        )
    }

    fun buildForSend(signature: ByteArray, preSignStructure: TonPreSignStructure): String {
        val signatures = DataVector()
        signatures.add(signature)

        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            preSignStructure.inputData,
            signatures,
            publicKeys,
        )

        val output = TheOpenNetwork.SigningOutput.parseFrom(compileWithSignatures)
        if (output.error != Common.SigningError.OK) {
            error("something went wrong")
        }

        return output.encoded
    }

    fun buildForSend(
        signature: ByteArray,
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        expireAt: Int,
        extras: TonTransactionExtras? = null,
    ): String {
        val input = buildInput(
            sequenceNumber = sequenceNumber,
            amount = amount,
            destination = destination,
            comment = extras?.memo.orEmpty(),
            expireAt = expireAt,
        )
        val txInputData = input.toByteArray()

        val signatures = DataVector()
        signatures.add(signature)

        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            txInputData,
            signatures,
            publicKeys,
        )

        val output = TheOpenNetwork.SigningOutput.parseFrom(compileWithSignatures)
        if (output.error != Common.SigningError.OK) {
            error("something went wrong")
        }

        return output.encoded
    }

    private fun buildInput(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        comment: String,
        expireAt: Int,
    ): TheOpenNetwork.SigningInput {
        val transfer = when (amount.type) {
            is AmountType.Coin -> makeTransfer(amount = amount, destination = destination, comment = comment)
            is AmountType.Token -> makeJettonTransfer(amount = amount, destination = destination, comment = comment)
            else -> throw BlockchainSdkError.FailedToBuildTx
        }

        return TheOpenNetwork.SigningInput
            .newBuilder()
            .addMessages(transfer)
            .setWalletVersion(TheOpenNetwork.WalletVersion.WALLET_V4_R2)
            .setSequenceNumber(sequenceNumber)
            .setExpireAt(expireAt)
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey))
            .build()
    }

    private fun makeTransfer(
        amount: Amount,
        destination: String,
        comment: String,
        jettonTransfer: TheOpenNetwork.JettonTransfer? = null,
    ): TheOpenNetwork.Transfer {
        return TheOpenNetwork.Transfer.newBuilder()
            .setDest(destination)
            .setAmount(ByteString.copyFrom(amount.longValue.bytes8BigEndian()))
            .setMode(modeTransactionConstant)
            .setBounceable(false)
            .setComment(comment)
            .also { transfer -> if (jettonTransfer != null) transfer.setJettonTransfer(jettonTransfer) }
            .build()
    }

    private fun makeJettonTransfer(amount: Amount, destination: String, comment: String): TheOpenNetwork.Transfer {
        val token = (amount.type as? AmountType.Token)?.token ?: throw BlockchainSdkError.FailedToBuildTx
        val jettonWalletAddress = jettonWalletAddresses[token] ?: throw BlockchainSdkError.FailedToBuildTx

        val amountValue = requireNotNull(amount.value) { "Amount value must not be null" }
        val jettonAmount = amountValue
            .movePointRight(amount.decimals)
            .toBigInteger()
            .toByteArray()
            .let(ByteString::copyFrom)
        val jettonTransfer = TheOpenNetwork.JettonTransfer.newBuilder()
            .setJettonAmount(jettonAmount)
            .setToOwner(destination)
            .setResponseAddress(walletAddress)
            // needs some amount to send "jetton transfer notification", use minimum
            .setForwardAmount(ByteString.copyFrom(1L.bytes8BigEndian()))
            .build()

        return makeTransfer(
            amount = Amount(JETTON_TRANSFER_PROCESSING_FEE, Blockchain.TON),
            destination = jettonWalletAddress,
            comment = comment,
            jettonTransfer = jettonTransfer,
        )
    }

    private fun parseMessage(transactionData: TonCompiledTransactionData): MessageRelaxed<Cell> {
        val decoded = Base64.decode(transactionData.message)
        val deserialized = BagOfCells.of(decoded).first()
        return messageRelaxedCodec.loadTlb(deserialized)
    }

    private fun MessageRelaxed<Cell>.extractComment(): String {
        val bodyCell = when (val body = this.body) {
            is Either.Left -> body.value
            is Either.Right -> body.value.value
        }
        return String(bodyCell.bits.toByteArray().removeLeadingZeros())
    }

    internal companion object {
        /* used to cover token transfer fees, commonly used value after TON fee reduction, actual costs now are ~10
         * times less, excess is returned
         */
        val JETTON_TRANSFER_PROCESSING_FEE = "0.05".toBigDecimal()

        /* used for token transfers when recipient's jetton wallet is already active */
        val JETTON_TRANSFER_PROCESSING_FEE_ACTIVE_WALLET = "0.0001".toBigDecimal()
    }
}