package com.tangem.blockchain.blockchains.xrp

import com.ripple.core.coretypes.AccountID
import com.ripple.core.coretypes.uint.UInt32
import com.ripple.crypto.ecdsa.ECDSASignature
import com.ripple.utils.HashUtils
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.override.XrpPayment
import com.tangem.blockchain.blockchains.xrp.override.XrpSignedTransaction
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bigIntegerValue
import org.bitcoinj.core.ECKey
import java.math.BigInteger
import com.ripple.core.coretypes.Amount as XrpAmount

@Suppress("MagicNumber")
class XrpTransactionBuilder(private val networkProvider: XrpNetworkProvider, publicKey: ByteArray) {
    var sequence: Long? = null
    // https://xrpl.org/blog/2021/reserves-lowered.html
    var minReserve = 10.toBigDecimal()
    val blockchain = Blockchain.XRP

    private val canonicalPublicKey = XrpAddressService.canonizePublicKey(publicKey)
    private var transaction: XrpSignedTransaction? = null

    suspend fun buildToSign(transactionData: TransactionData): Result<ByteArray> {
        val decodedXAddress = XrpAddressService.decodeXAddress(transactionData.destinationAddress)
        val destinationAddress = decodedXAddress?.address ?: transactionData.destinationAddress
        val xAddressDestinationTag = decodedXAddress?.destinationTag

        val destinationTag = if (transactionData.extras is XrpTransactionExtras) {
            val extrasTag = transactionData.extras.destinationTag
            if (xAddressDestinationTag != null && xAddressDestinationTag != extrasTag) {
                return Result.Failure(BlockchainSdkError.CustomError("Two distinct destination tags found"))
            }
            extrasTag
        } else {
            xAddressDestinationTag
        }

        if (!networkProvider.checkIsAccountCreated(destinationAddress) &&
            transactionData.amount.value!! < minReserve
        ) {
            return Result.Failure(
                BlockchainSdkError.CreateAccountUnderfunded(blockchain, Amount(minReserve, blockchain)),
            )
        }

        val payment = XrpPayment()
        payment.putTranslated(AccountID.Account, transactionData.sourceAddress)
        payment.putTranslated(AccountID.Destination, destinationAddress)
        payment.putTranslated(XrpAmount.Amount, transactionData.amount.bigIntegerValue().toString())
        payment.putTranslated(UInt32.Sequence, sequence)
        payment.putTranslated(XrpAmount.Fee, transactionData.fee!!.amount.bigIntegerValue().toString())
        if (destinationTag != null) {
            payment.putTranslated(UInt32.DestinationTag, destinationTag)
        }

        transaction = payment.prepare(canonicalPublicKey)

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            Result.Success(transaction!!.signingData)
        } else {
            Result.Success(HashUtils.halfSha512(transaction!!.signingData))
        }
    }

    fun buildToSend(signature: ByteArray): String {
        if (canonicalPublicKey[0] == 0xED.toByte()) {
            transaction!!.addSign(signature)
        } else {
            val derSignature = encodeDerSignature(signature)
            transaction!!.addSign(derSignature)
        }
        return transaction!!.tx_blob
    }

    fun getTransactionHash() = transaction?.hash?.bytes()

    private fun encodeDerSignature(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val ecdsaSignature = ECDSASignature(r, canonicalS)
        return ecdsaSignature.encodeToDER()
    }

    data class XrpTransactionExtras(val destinationTag: Long) : TransactionExtras
}
