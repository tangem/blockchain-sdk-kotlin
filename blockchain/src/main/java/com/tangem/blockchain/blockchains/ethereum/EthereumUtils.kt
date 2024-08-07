package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.eip712.EthEip712Util
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.isValidHex
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.remove
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toFixedLengthByteArray
import org.kethereum.extensions.transactions.encode
import org.kethereum.extensions.transactions.tokenTransferSignature
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.kethereum.model.createTransactionWithDefaults
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("MagicNumber", "LargeClass", "LongParameterList")
object EthereumUtils {
    private val tokenApproveSignature = "approve(address,uint256)".toByteArray().toKeccak().copyOf(4)
    private val processSignature =
        "process(address,uint256,bytes16,uint16)".toByteArray().toKeccak().copyOf(4) // 0x960cab120

    private const val HEX_PREFIX = "0x"
    private const val HEX_F = "f"

    // ERC-20 standard defines balanceOf function as returning uint256. Don't accept anything else.
    private const val UInt256Size = 32

    fun ByteArray.toKeccak(): ByteArray {
        return this.keccak()
    }

    internal fun parseEthereumDecimal(value: String, decimalsCount: Int): BigDecimal? {
        val data = value.removePrefix(HEX_PREFIX).asciiHexToBytes() ?: return null

        val balanceData = when {
            data.size <= UInt256Size -> data
            data.allOutOfRangeIsEqualTo(UInt256Size, 0) -> data.copyOf(UInt256Size)
            else -> return null
        }

        return balanceData
            .toHexString(prefix = "")
            .toBigIntegerOrNull(radix = 16)
            .toBigDecimalOrDefault()
            .movePointLeft(decimalsCount)
    }

    private fun String.asciiHexToBytes(): ByteArray? {
        var trimmedString = this.remove(" ")
        if (trimmedString.length % 2 != 0) {
            trimmedString = "0$trimmedString"
        }

        return if (trimmedString.isValidHex()) trimmedString.hexToBytes() else null
    }

    private fun ByteArray.allOutOfRangeIsEqualTo(range: Int, equal: Byte): Boolean =
        this.copyOfRange(range, this.size).all { it == equal }

    fun prepareSignedMessageData(signedHash: ByteArray, hashToSign: ByteArray, publicKey: ByteArray): String {
        val r = BigInteger(1, signedHash.copyOfRange(0, 32))
        val s = BigInteger(1, signedHash.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            hashToSign,
            PublicKey(publicKey.sliceArray(1..64)),
        )
        val v = (recId + 27).toBigInteger()

        return HEX_PREFIX +
            ecdsaSignature.r.toString(16) +
            ecdsaSignature.s.toString(16) +
            v.toString(16)
    }

    fun buildTransactionToSign(
        transactionData: TransactionData,
        nonce: BigInteger?,
        blockchain: Blockchain,
    ): CompiledEthereumTransaction? {
        transactionData.requireUncompiled()

        val extras = transactionData.extras as? EthereumTransactionExtras

        val nonceValue = extras?.nonce ?: nonce ?: return null

        val amount: BigDecimal = transactionData.amount.value ?: return null
        val transactionFee: BigDecimal = transactionData.fee?.amount?.value ?: return null

        val fee = transactionFee.movePointRight(transactionData.fee.amount.decimals).toBigInteger()
        val bigIntegerAmount =
            amount.movePointRight(transactionData.amount.decimals).toBigInteger()

        val to: Address
        val value: BigInteger
        val input: ByteArray // data for smart contract

        if (transactionData.amount.type == AmountType.Coin) { // coin transfer
            to = Address(transactionData.destinationAddress)
            value = bigIntegerAmount
            input = ByteArray(0)
        } else { // token transfer (or approve)
            to = Address(
                transactionData.contractAddress
                    ?: error("Contract address is not specified!"),
            )
            value = BigInteger.ZERO
            input =
                createErc20TransferData(transactionData.destinationAddress, bigIntegerAmount)
        }

        val gasLimitToUse =
            extras?.gasLimit ?: (transactionData.fee as? Fee.Ethereum)?.gasLimit ?: DEFAULT_GAS_LIMIT

        val transaction = createTransactionWithDefaults(
            from = Address(transactionData.sourceAddress),
            to = to,
            value = value,
            gasPrice = fee.divide(gasLimitToUse),
            gasLimit = gasLimitToUse,
            nonce = nonceValue,
            input = extras?.data ?: input, // use data from extras prefer (TODO refactor this)
        )
        val chainId = blockchain.getChainId()
            ?: error("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")
        val hash = transaction
            .encode(SignatureData(v = chainId.toBigInteger()))
            .keccak()
        return CompiledEthereumTransaction(transaction, hash)
    }

    fun prepareTransactionToSend(
        signature: ByteArray,
        transactionToSign: CompiledEthereumTransaction,
        walletPublicKey: Wallet.PublicKey,
        blockchain: Blockchain,
    ): ByteArray {
        val publicKey = walletPublicKey.blockchainKey.toDecompressedPublicKey().sliceArray(1..64)
        return prepareTransactionToSend(signature, transactionToSign, publicKey, blockchain)
    }

    fun prepareTransactionToSend(
        signature: ByteArray,
        transactionToSign: CompiledEthereumTransaction,
        walletPublicKey: ByteArray,
        blockchain: Blockchain,
    ): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            transactionToSign.hash,
            PublicKey(walletPublicKey),
        )
        val chainId = blockchain.getChainId()
            ?: error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
        val v = (recId + 27 + 8 + chainId * 2).toBigInteger() // EIP-155
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return transactionToSign.transaction.encode(signatureData)
    }

    // TODO: https://tangem.atlassian.net/browse/AND-5811 Replace with SmartContractMethod interface implementations
    fun createErc20ApproveDataHex(spender: String, amount: Amount?): String = createErc20ApproveData(
        spender = spender,
        amount = amount?.value?.movePointRight(amount.decimals)?.toBigInteger(),
    ).toHexString()

    private fun createErc20TransferData(recipient: String, amount: BigInteger) = tokenTransferSignature.toByteArray() +
        recipient.substring(2).hexToBytes().toFixedLengthByteArray(32) +
        amount.toBytesPadded(32)

    // TODO: https://tangem.atlassian.net/browse/AND-5811 Replace with SmartContractMethod interface implementations
    internal fun createErc20TransferData(recepient: String, amount: Amount) = createErc20TransferData(
        recepient,
        amount.value!!.movePointRight(amount.decimals).toBigInteger(),
    )

    private fun createErc20ApproveData(spender: String, amount: BigInteger?): ByteArray = tokenApproveSignature +
        spender.substring(2).hexToBytes().toFixedLengthByteArray(32) +
        (amount?.toBytesPadded(32) ?: HEX_F.repeat(64).hexToBytes())

    fun createProcessData(cardAddress: String, amount: BigInteger, otp: ByteArray, sequence: Int): ByteArray {
        val cardAddressBytes = cardAddress.substring(2).hexToBytes().toFixedLengthByteArray(32)
        val amountBytes = amount.toBytesPadded(32)
        val otpBytes = otp.copyOf(16) + ByteArray(16)
        val sequenceBytes = BigInteger.valueOf(sequence.toLong()).toBytesPadded(32)

        return processSignature + cardAddressBytes + amountBytes + otpBytes + sequenceBytes
    }

    fun makeTypedDataHash(rawMessage: String): ByteArray {
        return EthEip712Util.eip712Hash(rawMessage)
    }
}
