package com.tangem.blockchain.blockchains.binance

import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.TransactionOption
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.Transfer
import com.tangem.blockchain.blockchains.binance.client.encoding.message.MessageType
import com.tangem.blockchain.blockchains.binance.client.encoding.message.TransactionRequestAssemblerExtSign
import com.tangem.blockchain.blockchains.binance.client.encoding.message.TransferMessage
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import java.math.BigInteger

class BinanceTransactionBuilder(
    publicKey: ByteArray,
    isTestNet: Boolean = false,
) {
    var accountNumber: Long? = null
    var sequence: Long? = null

    private val chainId = BinanceChain.getChain(isTestNet).value

    @Suppress("MagicNumber")
    private val prefixedPubKey = MessageType.PubKey.typePrefixBytes + 33.toByte() + publicKey.toCompressedPublicKey()

    private var transactionAssembler: TransactionRequestAssemblerExtSign? = null
    private var transferMessage: TransferMessage? = null

    fun buildToSign(transactionData: TransactionData): Result<ByteArray> {
        transactionData.requireUncompiled()

        val amount = transactionData.amount

        if (!amount.isAboveZero()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Transaction amount is not defined"),
            )
        }
        val accountNumber = accountNumber ?: return Result.Failure(BlockchainSdkError.CustomError("No account number"))
        val sequence = sequence ?: return Result.Failure(BlockchainSdkError.CustomError("No sequence"))

        val transfer = Transfer()
        transfer.coin = if (amount.type is AmountType.Token) {
            amount.type.token.contractAddress
        } else {
            amount.currencySymbol
        }
        transfer.fromAddress = transactionData.sourceAddress
        transfer.toAddress = transactionData.destinationAddress
        transfer.amount = transactionData.amount.value!!
            .setScale(Blockchain.Binance.decimals()).toPlainString()

        val options = TransactionOption.DEFAULT_INSTANCE
        options.memo = (transactionData.extras as? BinanceTransactionExtras)?.memo ?: ""

        val accountData = BinanceAccountData(chainId, accountNumber, sequence)

        transactionAssembler = TransactionRequestAssemblerExtSign(accountData, prefixedPubKey, options)
        transferMessage = transactionAssembler!!.createTransferMessage(transfer)

        return Result.Success(transactionAssembler!!.prepareForSign(transferMessage).calculateSha256())
    }

    @Suppress("MagicNumber")
    fun buildToSend(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s

        // bigIntegerToBytes cuts leading zero if present
        val canonicalSignature =
            Utils.bigIntegerToBytes(r, 32) + Utils.bigIntegerToBytes(canonicalS, 32)
        val encodedSignature = transactionAssembler!!.encodeSignature(canonicalSignature)

        val encodedTransferMessage = transactionAssembler!!.encodeTransferMessage(transferMessage)

        return transactionAssembler!!.encodeStdTx(encodedTransferMessage, encodedSignature)
    }
}

data class BinanceAccountData(
    val chainId: String,
    val accountNumber: Long,
    val sequence: Long,
)

data class BinanceTransactionExtras(val memo: String) : TransactionExtras
