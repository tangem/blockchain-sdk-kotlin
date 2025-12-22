package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.eip712.EthEip712Util
import com.tangem.blockchain.blockchains.ethereum.models.EthereumCompiledTransaction
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.*
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.remove
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.extensions.transactions.encode
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.*
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("MagicNumber", "LargeClass", "LongParameterList")
object EthereumUtils {
    private const val HEX_PREFIX = "0x"

    const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

    const val HEX_CHARS_PER_BYTE = 2

    // Each "word" in Ethereum ABI is 32 bytes, which equals 64 hex characters
    const val WORD_BYTES = 32
    const val WORD_HEX_LENGTH = WORD_BYTES * HEX_CHARS_PER_BYTE // 64

    // Ethereum address is 20 bytes, equals 40 hex characters
    const val ADDRESS_BYTES = 20
    const val ADDRESS_HEX_LENGTH = ADDRESS_BYTES * HEX_CHARS_PER_BYTE // 40

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

        // TODO refactor [REDACTED_TASK_KEY]
        when (val amountType = transactionData.amount.type) {
            is AmountType.TokenYieldSupply, // Destination is defined by application
            AmountType.Coin,
            -> { // coin transfer
                to = Address(transactionData.destinationAddress)
                value = bigIntegerAmount
                input = extras.callData?.data ?: ByteArray(0) // use empty ByteArray
            }
            is AmountType.Token -> { // token transfer (or approve)
                to = Address(amountType.token.contractAddress)
                value = BigInteger.ZERO
                input = extras.callData?.data ?: error("Call data is not specified")
            }
            else -> throw BlockchainSdkError.CustomError("Not implemented")
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
        return prepareTransactionToSend(
            signature = signature,
            transactionToSign = transactionToSign,
            walletPublicKey = walletPublicKey.blockchainKey.toDecompressedPublicKey(),
            blockchain = blockchain,
        )
    }

    fun prepareTransactionToSend(
        signature: ByteArray,
        transactionToSign: EthereumCompiledTxInfo.Legacy,
        walletPublicKey: ByteArray,
        blockchain: Blockchain,
    ): ByteArray {
        val extendedSignature = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = transactionToSign.hash,
            publicKey = walletPublicKey,
        )

        val chainId = blockchain.getChainId()
            ?: error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")

        val v = (extendedSignature.recId + 27 + 8 + chainId * 2).toBigInteger() // EIP-155
        val signatureData = SignatureData(extendedSignature.r, extendedSignature.s, v)

        return transactionToSign.transaction.encode(signatureData)
    }

    fun makeTypedDataHash(rawMessage: String): ByteArray {
        return EthEip712Util.eip712Hash(rawMessage)
    }

    fun String.parseEthereumAddress(): String {
        return hexToFixedSizeBytes().removeLeadingZeros().toHexString().formatHex().lowercase()
    }
}