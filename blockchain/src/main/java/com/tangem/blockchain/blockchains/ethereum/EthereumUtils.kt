package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.*
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.eip712.MoshiAdapter
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toFixedLengthByteArray
import org.kethereum.extensions.transactions.encode
import org.kethereum.extensions.transactions.tokenTransferSignature
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.kethereum.model.createTransactionWithDefaults
import pm.gnosis.eip712.typedDataHash
import java.math.BigDecimal
import java.math.BigInteger

class EthereumUtils {
    companion object {
        private val tokenApproveSignature = "approve(address,uint256)".toByteArray().toKeccak().copyOf(4)
        private val setSpendLimitSignature = "setSpendLimit(address,uint256)".toByteArray().toKeccak().copyOf(4)
        private val initOTPSignature = "initOTP(bytes16,uint16)".toByteArray().toKeccak().copyOf(4) // 0x0ac81ec3
        private val setWalletSignature = "setWallet(address)".toByteArray().toKeccak().copyOf(4) // 0xdeaa59df
        private val processSignature = "process(address,uint256,bytes16,uint16)".toByteArray().toKeccak().copyOf(4)// 0x960cab120
        private val tokenTransferFromSignature = "transferFrom(address,address,uint256)".toByteArray().toKeccak().copyOf(4)

        private const val HEX_PREFIX = "0x"

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

            return HEX_PREFIX +
                ecdsaSignature.r.toString(16) +
                ecdsaSignature.s.toString(16) +
                v.toString(16)
        }

        fun buildTransactionToSign(
            transactionData: TransactionData,
            nonce: BigInteger?,
            blockchain: Blockchain
        ): CompiledEthereumTransaction? {

            val extras = transactionData.extras as? EthereumTransactionExtras

            val nonceValue = extras?.nonce ?: nonce ?: return null

            val amount: BigDecimal = transactionData.amount.value ?: return null
            val transactionFee: BigDecimal = transactionData.fee?.amount?.value ?: return null

            val fee = transactionFee.movePointRight(transactionData.fee.amount.decimals).toBigInteger()
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

            val gasLimitToUse =
                extras?.gasLimit ?: (transactionData.fee as? Fee.Ethereum)?.gasLimit ?: DEFAULT_GAS_LIMIT

            val transaction = createTransactionWithDefaults(
                from = Address(transactionData.sourceAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimitToUse),
                gasLimit = gasLimitToUse,
                nonce = nonceValue,
                input = extras?.data ?: input
            )
            val chainId = blockchain.getChainId()
                ?: throw Exception("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")
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
                PublicKey(walletPublicKey)
            )
            val chainId = blockchain.getChainId()
                ?: error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
            val v = (recId + 27 + 8 + (chainId * 2)).toBigInteger() // EIP-155
            val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

            return transactionToSign.transaction.encode(signatureData)
        }

        fun buildApproveToSign(
            transactionData: TransactionData,
            nonce: BigInteger?,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
        ): CompiledEthereumTransaction? {
            if (transactionData.amount.type == AmountType.Coin) return null

            val extras = transactionData.extras as? EthereumTransactionExtras
            val nonceValue = extras?.nonce ?: nonce ?: return null
            val amount: BigDecimal = transactionData.amount.value ?: return null
            val transactionFee: BigDecimal = transactionData.fee?.amount?.value ?: return null

            val fee = transactionFee.movePointRight(transactionData.fee.amount.decimals).toBigInteger()
            val bigIntegerAmount = amount.movePointRight(transactionData.amount.decimals).toBigInteger()

            val to = Address(transactionData.contractAddress
                ?: throw Exception("Contract address is not specified!"))
            val value = BigInteger.ZERO
            val input = createErc20ApproveData(transactionData.destinationAddress, bigIntegerAmount)

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
                .encode(SignatureData(v = chainId.toBigInteger()))
                .keccak()

            return CompiledEthereumTransaction(transaction, hash)
        }

        fun buildSetSpendLimitToSign(
            processorContractAddress: String,
            cardAddress: String,
            amount: Amount,
            transactionFee: Amount?,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
            nonce: BigInteger?,
        ): CompiledEthereumTransaction? {
            if (transactionFee?.value == null) return null
            val fee = transactionFee.value?.movePointRight(transactionFee.decimals)?.toBigInteger() ?: return null
            val bigIntegerAmount = amount.value?.movePointRight(amount.decimals)?.toBigInteger() ?: return null
            val nonceValue = nonce ?: return null

            val to = Address(processorContractAddress)
            val value = BigInteger.ZERO
            val input: ByteArray = createSetSpendLimitData(cardAddress, bigIntegerAmount)

            val gasLimitToUse = gasLimit ?: return null

            val transaction = createTransactionWithDefaults(
                from = Address(cardAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimit),
                gasLimit = gasLimitToUse,
                nonce = nonceValue,
                input = input
            )
            val chainId = blockchain.getChainId()
                ?: throw Exception("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")

            val hash = transaction
                .encode(SignatureData(v = chainId.toBigInteger()))
                .keccak()

            return CompiledEthereumTransaction(transaction, hash)
        }

        fun buildInitOTPToSign(
            processorContractAddress: String,
            cardAddress: String,
            otp: ByteArray,
            otpCounter: Int,
            transactionFee: Amount?,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
            nonce: BigInteger?,
        ): CompiledEthereumTransaction? {
            if (transactionFee?.value == null) return null
            val fee = transactionFee.value?.movePointRight(transactionFee.decimals)?.toBigInteger() ?: return null
            val nonceValue = nonce ?: return null

            val to = Address(processorContractAddress)
            val value = BigInteger.ZERO
            val input: ByteArray = createInitOTPData(otp, otpCounter)

            val gasLimitToUse = gasLimit ?: return null

            val transaction = createTransactionWithDefaults(
                from = Address(cardAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimit),
                gasLimit = gasLimitToUse,
                nonce = nonceValue,
                input = input
            )

            val chainId = blockchain.getChainId()
                ?: throw Exception("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")

            val hash = transaction
                .encode(SignatureData(v = chainId.toBigInteger()))
                .keccak()

            return CompiledEthereumTransaction(transaction, hash)
        }


        fun buildSetWalletToSign(
            processorContractAddress: String,
            cardAddress: String,
            transactionFee: Amount?,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
            nonce: BigInteger?,
        ): CompiledEthereumTransaction? {
            val fee = transactionFee?.value?.movePointRight(transactionFee.decimals)?.toBigInteger() ?: return null
            val nonceValue = nonce ?: return null

            val to = Address(processorContractAddress)
            val value = BigInteger.ZERO
            val input: ByteArray = createSetWalletData(cardAddress)

            val gasLimitToUse = gasLimit ?: return null

            val transaction = createTransactionWithDefaults(
                from = Address(cardAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimit),
                gasLimit = gasLimitToUse,
                nonce = nonceValue,
                input = input
            )

            val chainId = blockchain.getChainId()
                ?: throw Exception("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")

            val hash = transaction
                .encode(SignatureData(v = chainId.toBigInteger()))
                .keccak()

            return CompiledEthereumTransaction(transaction, hash)
        }


        fun buildProcessToSign(
            processorContractAddress: String,
            processorAddress: String,
            cardAddress: String,
            amount: Amount,
            transactionFee: Amount,
            otp: ByteArray,
            otpCounter: Int,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
            nonce: BigInteger?
        ): CompiledEthereumTransaction? {
            val fee = transactionFee.value?.movePointRight(transactionFee.decimals)?.toBigInteger() ?: return null
            val bigIntegerAmount = amount.value?.movePointRight(amount.decimals)?.toBigInteger() ?: return null
            val nonceValue = nonce ?: return null

            val to = Address(processorContractAddress)
            val value = BigInteger.ZERO

            val gasLimitToUse = gasLimit ?: return null

            val input: ByteArray = createProcessData(cardAddress, bigIntegerAmount, otp, otpCounter.and(0xFFFF))

            val transaction = createTransactionWithDefaults(
                from = Address(processorAddress),
                to = to,
                value = value,
                gasPrice = fee.divide(gasLimit),
                gasLimit = gasLimitToUse,
                nonce = nonceValue,
                input = input
            )

            val chainId = blockchain.getChainId()
                ?: throw Exception("${blockchain.fullName} blockchain is not supported by Ethereum Wallet Manager")

            val hash = transaction
                .encode(SignatureData(v = chainId.toBigInteger()))
                .keccak()

            return CompiledEthereumTransaction(transaction, hash)
        }

        fun buildTransferFromToSign(
            transactionData: TransactionData,
            nonce: BigInteger?,
            blockchain: Blockchain,
            gasLimit: BigInteger?,
        ): CompiledEthereumTransaction? {

            val extras = transactionData.extras as? EthereumTransactionExtras

            val nonceValue = extras?.nonce ?: nonce ?: return null

            val amount: BigDecimal = transactionData.amount.value ?: return null
            val transactionFee: BigDecimal = transactionData.fee?.amount?.value ?: return null

            val fee = transactionFee.movePointRight(transactionData.fee.amount.decimals).toBigInteger()
            val bigIntegerAmount =
                amount.movePointRight(transactionData.amount.decimals).toBigInteger()

            val to: Address
            val value: BigInteger
            val input: ByteArray //data for smart contract

            if (transactionData.amount.type == AmountType.Coin) { //coin transfer
                return null
            } else { //token transfer from
                to = Address(transactionData.contractAddress
                    ?: throw Exception("Contract address is not specified!"))
                value = BigInteger.ZERO
                input =
                    createErc20TransferFromData(transactionData.sourceAddress, transactionData.destinationAddress, bigIntegerAmount)
            }

            val gasLimitToUse = extras?.gasLimit ?: gasLimit ?: return null

            val transaction = createTransactionWithDefaults(
                from = Address(transactionData.destinationAddress),
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
                .encode(SignatureData(v = chainId.toBigInteger()))
                .keccak()
            return CompiledEthereumTransaction(transaction, hash)
        }

        private fun createErc20TransferData(recipient: String, amount: BigInteger) =
            tokenTransferSignature.toByteArray() +
                recipient.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                amount.toBytesPadded(32)

        internal fun createErc20TransferData(recepient: String, amount: Amount) =
            createErc20TransferData(
                recepient, amount.value!!.movePointRight(amount.decimals).toBigInteger()
            )

        private fun createErc20ApproveData(spender: String, amount: BigInteger) =
            tokenApproveSignature +
                spender.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                amount.toBytesPadded(32)

        internal fun createErc20ApproveData(spender: String, amount: Amount) =
            createErc20ApproveData(
                spender = spender,
                amount = amount.value!!.movePointRight(amount.decimals).toBigInteger()
            )

        private fun createSetSpendLimitData(cardAddress: String, amount: BigInteger) =
            setSpendLimitSignature +
                cardAddress.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                amount.toBytesPadded(32)

        internal fun createSetSpendLimitData(cardAddress: String, amount: Amount) =
            createSetSpendLimitData(
                cardAddress = cardAddress,
                amount = amount.value!!.movePointRight(amount.decimals).toBigInteger()
            )

        internal fun createInitOTPData(otp: ByteArray, otpCounter: Int) =
            initOTPSignature +
                otp.copyOf(16) + ByteArray(16 + 30) +
                otpCounter.toByteArray(2)

        fun createSetWalletData(address: String) =
            setWalletSignature +
                address.substring(2).hexToBytes().toFixedLengthByteArray(32)

        private fun createErc20TransferFromData(source: String, destination: String, amount: BigInteger) =
            tokenTransferFromSignature +
                source.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                destination.substring(2).hexToBytes().toFixedLengthByteArray(32) +
                amount.toBytesPadded(32)

        fun createProcessData(
            cardAddress: String,
            amount: BigInteger,
            otp: ByteArray,
            sequence: Int
        ): ByteArray {
            val cardAddressBytes = cardAddress.substring(2).hexToBytes().toFixedLengthByteArray(32)
            val amountBytes = amount.toBytesPadded(32)
            val otpBytes = otp.copyOf(16) + ByteArray(16)
            val sequenceBytes = BigInteger.valueOf(sequence.toLong()).toBytesPadded(32)

            return processSignature + cardAddressBytes + amountBytes + otpBytes + sequenceBytes
        }

        fun makeTypedDataHash(rawMessage: String): ByteArray {
            val messageParsed = TangemEIP712JsonParser(MoshiAdapter()).parseMessage(rawMessage)
           return typedDataHash(messageParsed.message, messageParsed.domain)
        }
    }
}
