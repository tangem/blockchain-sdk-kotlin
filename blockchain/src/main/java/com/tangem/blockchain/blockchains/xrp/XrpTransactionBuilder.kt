package com.tangem.blockchain.blockchains.xrp

import com.ripple.core.coretypes.AccountID
import com.ripple.core.coretypes.uint.UInt32
import com.ripple.crypto.ecdsa.ECDSASignature
import com.ripple.utils.HashUtils
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.blockchain.blockchains.xrp.override.XrpPayment
import com.tangem.blockchain.blockchains.xrp.override.XrpSignedTransaction
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.CreateAccountUnderfunded
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bigIntegerValue
import org.bitcoinj.core.ECKey
import java.math.BigInteger
import com.ripple.core.coretypes.Amount as XrpAmount

class XrpTransactionBuilder(private val networkManager: XrpNetworkManager, publicKey: ByteArray) {
    var sequence: Long? = null
    var minReserve = 20.toBigDecimal()
    val blockchain = Blockchain.XRP

    private val canonicalPublicKey = XrpAddressService.canonizePublicKey(publicKey)
    private var transaction: XrpSignedTransaction? = null

    suspend fun buildToSign(transactionData: TransactionData): Result<ByteArray> {
        if (!networkManager.checkIsAccountCreated(transactionData.destinationAddress)
                && transactionData.amount.value!! < minReserve) {
            return Result.Failure(
                    CreateAccountUnderfunded(Amount(minReserve, blockchain))
            )
        }
        val payment = XrpPayment()
        payment.putTranslated(AccountID.Account, transactionData.sourceAddress)
        payment.putTranslated(AccountID.Destination, transactionData.destinationAddress)
        payment.putTranslated(XrpAmount.Amount, transactionData.amount.bigIntegerValue().toString())
        payment.putTranslated(UInt32.Sequence, sequence)
        payment.putTranslated(XrpAmount.Fee, transactionData.fee!!.bigIntegerValue().toString())

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
}