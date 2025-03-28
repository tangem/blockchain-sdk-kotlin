package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.eip712.EthEip712Util
import com.tangem.blockchain.blockchains.ethereum.models.EthereumCompiledTransaction
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.isValidHex
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.remove
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.transactions.encode
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.*
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("MagicNumber", "LargeClass", "LongParameterList")
object EthereumUtils {
    private const val HEX_PREFIX = "0x"

    // ERC-20 standard defines balanceOf function as returning uint256. Don't accept anything else.
    private const val UInt256Size = 32

    private val compiledTransactionAdapter = moshi.adapter(EthereumCompiledTransaction::class.java)

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
        blockchain: Blockchain,
    ): EthereumCompiledTxInfo.Legacy {
        val transaction = when (transactionData) {
            is TransactionData.Uncompiled -> buildUncompiledTransactionToSign(transactionData)
            is TransactionData.Compiled -> buildCompiledTransactionToSign(transactionData)
        } ?: error("Error while building transaction to sign")

        val chainId = blockchain.getChainId()
            ?: error("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")
        val hash = transaction
            .encode(SignatureData(v = chainId.toBigInteger()))
            .keccak()

        return EthereumCompiledTxInfo.Legacy(
            hash = hash,
            transaction = transaction,
        )
    }

    private fun buildCompiledTransactionToSign(transactionData: TransactionData.Compiled): Transaction {
        val compiledTransaction = if (transactionData.value is TransactionData.Compiled.Data.RawString) {
            transactionData.value.data
        } else {
            error("Compiled transaction must be in hex format")
        }

        val parsed = compiledTransactionAdapter.fromJson(compiledTransaction)
            ?: error("Unable to parse compiled transaction")

        val value = parsed.value?.hexToBigDecimal()?.toBigInteger() ?: BigInteger.ZERO

        val fee = transactionData.fee?.amount?.value
            ?.movePointRight(transactionData.fee.amount.decimals)
            ?.toBigInteger()?.takeIf { it > BigInteger.ZERO }
            ?: error("Transaction fee must be specified")

        val gasLimit = parsed.gasLimit.hexToBigDecimal().toBigInteger().takeIf { it > BigInteger.ZERO }
            ?: error("Transaction fee must be specified")

        val gasPrice = parsed.gasPrice?.hexToBigDecimal()?.toBigInteger() ?: fee.divide(gasLimit)
        val maxPriorityFeePerGas = parsed.maxPriorityFeePerGas?.hexToBigDecimal()?.toBigInteger()
        val maxFeePerGas = parsed.maxFeePerGas?.hexToBigDecimal()?.toBigInteger()

        return createTransactionWithDefaults(
            from = Address(parsed.from),
            to = Address(parsed.to),
            gasPrice = gasPrice,
            value = value,
            gasLimit = gasLimit,
            nonce = parsed.nonce.toBigInteger(),
            input = parsed.data.hexToBytes(),
            chain = ChainId(parsed.chainId.toBigInteger()),
            maxPriorityFeePerGas = maxPriorityFeePerGas,
            maxFeePerGas = maxFeePerGas,
        )
    }

    private fun buildUncompiledTransactionToSign(transactionData: TransactionData.Uncompiled): Transaction? {
        val extras = transactionData.extras as? EthereumTransactionExtras

        val nonceValue = extras?.nonce ?: return null

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
                transactionData.contractAddress ?: error("Contract address is not specified!"),
            )
            value = BigInteger.ZERO
            input = extras.smartContract?.data ?: error("Smart contract is not specified")
        }

        val gasLimitToUse = extras.gasLimit
            ?: (transactionData.fee as? Fee.Ethereum.Legacy)?.gasLimit
            ?: DEFAULT_GAS_LIMIT

        return createTransactionWithDefaults(
            from = Address(transactionData.sourceAddress),
            to = to,
            value = value,
            gasPrice = fee.divide(gasLimitToUse),
            gasLimit = gasLimitToUse,
            nonce = nonceValue,
            input = input,
        )
    }

    fun prepareTransactionToSend(
        signature: ByteArray,
        transactionToSign: EthereumCompiledTxInfo.Legacy,
        walletPublicKey: Wallet.PublicKey,
        blockchain: Blockchain,
    ): ByteArray {
        val publicKey = walletPublicKey.blockchainKey.toDecompressedPublicKey().sliceArray(1..64)
        return prepareTransactionToSend(signature, transactionToSign, publicKey, blockchain)
    }

    fun prepareTransactionToSend(
        signature: ByteArray,
        transactionToSign: EthereumCompiledTxInfo.Legacy,
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

    fun makeTypedDataHash(rawMessage: String): ByteArray {
        return EthEip712Util.eip712Hash(rawMessage)
    }
}