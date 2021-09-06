package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toFixedLengthByteArray
import org.kethereum.extensions.transactions.encodeRLP
import org.kethereum.extensions.transactions.tokenTransferSignature
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.kethereum.model.createTransactionWithDefaults
import java.math.BigDecimal
import java.math.BigInteger

class EthereumUtils {
    companion object {

        fun ByteArray.toKeccak(): ByteArray {
            return this.keccak()
        }

        fun prepareSignedMessageData(
            signedHash: ByteArray,
            hashToSign: ByteArray,
            publicKey: ByteArray,
        ): String {
            val r = BigInteger(1, signedHash.copyOfRange(0, 32))
            val s = BigInteger(1, signedHash.copyOfRange(32, 64))

            val ecdsaSignature = ECDSASignature(r, s).canonicalise()

            val recId = ecdsaSignature.determineRecId(hashToSign,
                PublicKey(publicKey.sliceArray(1..64)))
            val v = (recId + 27).toBigInteger()

            return HEX_PREFIX + ecdsaSignature.r.toString(16) + ecdsaSignature.s.toString(16) +
                    v.toString(16)
        }

        fun buildTransactionToSign(
            transactionData: TransactionData,
            nonce: BigInteger?,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
        ): TransactionToSign? {

            val extras = transactionData.extras as? EthereumTransactionExtras

            val nonceValue = extras?.nonce ?: nonce ?: return null

            val amount: BigDecimal = transactionData.amount.value ?: return null
            val transactionFee: BigDecimal = transactionData.fee?.value ?: return null

            val fee = transactionFee.movePointRight(transactionData.fee.decimals).toBigInteger()
            val bigIntegerAmount =
                amount.movePointRight(transactionData.amount.decimals).toBigInteger()

            val to: Address
            val value: BigInteger
            val input: ByteArray //data for smart contract

            if (transactionData.amount.type == AmountType.Coin) { //coin transfer
                to = Address(transactionData.destinationAddress)
                value = bigIntegerAmount
                input = ByteArray(0)
            } else { //token transfer
                to = Address(transactionData.contractAddress
                    ?: throw Exception("Contract address is not specified!"))
                value = BigInteger.ZERO
                input =
                    createErc20TransferData(transactionData.destinationAddress, bigIntegerAmount)
            }

            val gasLimitToUse = extras?.gasLimit ?: gasLimit ?: return null

            val transaction = createTransactionWithDefaults(
                from = Address(transactionData.sourceAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimitToUse),
                gasLimit = extras?.gasLimit ?: gasLimitToUse,
                nonce = nonceValue,
                input = extras?.data ?: input
            )
            val chainId = blockchain.getChainId()
                ?: throw Exception("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")
            val hash = transaction
                .encodeRLP(SignatureData(v = chainId.toBigInteger()))
                .keccak()
            return TransactionToSign(transaction, hash)
        }

        private fun createErc20TransferData(recepient: String, amount: BigInteger) =
            tokenTransferSignature.toByteArray() +
                    recepient.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                    amount.toBytesPadded(32)

        internal fun createErc20TransferData(recepient: String, amount: Amount) =
            createErc20TransferData(
                recepient, amount.value!!.movePointRight(amount.decimals).toBigInteger()
            )

        private const val HEX_PREFIX = "0x"
    }
}