package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.google.protobuf.ByteString
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.createErc20TransferData
import com.tangem.blockchain.blockchains.ethereum.models.EthereumCompiledTransaction
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.toByteArray
import org.ton.bigint.isZero
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.Ethereum
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.math.BigInteger

/**
 * Ethereum TW transaction builder
 *
 * @property wallet wallet
 */
internal class EthereumTWTransactionBuilder(wallet: Wallet) : EthereumTransactionBuilder(wallet = wallet) {

    private val coinType = CoinType.ETHEREUM
    private val chainId = wallet.blockchain.getChainId()
        ?: error("Invalid chain id for ${wallet.blockchain.name} blockchain")

    @OptIn(ExperimentalStdlibApi::class)
    private val compiledTransactionAdapter by lazy { moshi.adapter<EthereumCompiledTransaction>() }

    override fun buildForSign(transaction: TransactionData): EthereumCompiledTxInfo.TWInfo {
        val input = buildSigningInput(transaction)
        val preSigningOutput = buildTxCompilerPreSigningOutput(input)
        return EthereumCompiledTxInfo.TWInfo(hash = preSigningOutput.dataHash.toByteArray(), input = input)
    }

    override fun buildForSend(
        transaction: TransactionData,
        signature: ByteArray,
        compiledTransaction: EthereumCompiledTxInfo,
    ): ByteArray {
        val output = buildSigningOutput(
            // TODO: It's required to use input that we build for signing cause bad EthereumWalletManager's architecture
            input = (compiledTransaction as EthereumCompiledTxInfo.TWInfo).input,
            hash = compiledTransaction.hash,
            signature = signature,
        )

        return output.encoded.toByteArray()
    }

    override fun buildDummyTransactionForL1(
        amount: Amount,
        destination: String,
        data: String?,
        fee: Fee.Ethereum,
    ): ByteArray {
        val eip1559Fee = fee as Fee.Ethereum.EIP1559

        val input = buildSigningInput(
            chainId = chainId,
            destinationAddress = destination,
            coinAmount = BigInteger.ONE,
            fee = eip1559Fee,
            extras = EthereumTransactionExtras(nonce = BigInteger.ONE),
        )

        val preSigningOutput = buildTxCompilerPreSigningOutput(input)
        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(transaction: TransactionData, hash: ByteArray, signature: ByteArray): ByteArray {
        val input = buildSigningInput(transaction)
        val output = buildSigningOutput(input = input, hash = hash, signature = signature)
        return output.encoded.toByteArray()
    }

    private fun buildSigningInput(transaction: TransactionData): Ethereum.SigningInput {
        return when (transaction) {
            is TransactionData.Compiled -> buildCompiledSingingInput(transaction)
            is TransactionData.Uncompiled -> buildUncompiledSigningInput(transaction)
        }
    }

    private fun buildUncompiledSigningInput(transaction: TransactionData.Uncompiled): Ethereum.SigningInput {
        val amountValue = transaction.amount.value?.movePointRight(transaction.amount.decimals)
            ?.toBigInteger()
            ?: throw BlockchainSdkError.CustomError("Fail to parse amount")

        val ethereumFee = transaction.fee as? Fee.Ethereum ?: throw BlockchainSdkError.CustomError("Invalid fee")
        val extras = transaction.extras as? EthereumTransactionExtras

        return when (transaction.amount.type) {
            AmountType.Coin -> buildSigningInput(
                chainId = chainId,
                destinationAddress = transaction.destinationAddress,
                coinAmount = amountValue,
                fee = ethereumFee,
                extras = extras,
            )
            is AmountType.Token -> {
                buildSigningInput(
                    chainId = chainId,
                    destinationAddress = transaction.amount.type.token.contractAddress,
                    coinAmount = BigInteger.ZERO,
                    fee = ethereumFee,
                    extras = if (extras != null && extras.data == null) {
                        val transferData = createErc20TransferData(
                            recipient = transaction.destinationAddress,
                            amount = amountValue,
                        )

                        EthereumTransactionExtras(data = transferData, nonce = extras.nonce)
                    } else {
                        extras
                    },
                )
            }
            else -> throw BlockchainSdkError.CustomError("Not implemented")
        }
    }

    private fun buildCompiledSingingInput(transaction: TransactionData.Compiled): Ethereum.SigningInput {
        val compiledTransaction = (transaction.value as? TransactionData.Compiled.Data.RawString)?.data
            ?: error("Compiled transaction must be in hex format")

        val parsed = compiledTransactionAdapter.fromJson(compiledTransaction)
            ?: error("Unable to parse compiled transaction")

        val amount = parsed.value?.hexToBigDecimal()?.toBigInteger() ?: BigInteger.ZERO
        val nonce = parsed.nonce.toBigInteger()

        return Ethereum.SigningInput.newBuilder()
            .setChainId(ByteString.copyFrom(chainId.toByteArray()))
            .setNonce(ByteString.copyFrom(nonce.toByteArray()))
            .setDestinationAddress(address = parsed.to)
            .setFeeParams(parsed = parsed)
            .setTransaction(coinAmount = amount, data = parsed.data.hexToBytes())
            .build()
    }

    private fun buildSigningInput(
        chainId: Int,
        destinationAddress: String,
        coinAmount: BigInteger,
        fee: Fee.Ethereum,
        extras: EthereumTransactionExtras?,
    ): Ethereum.SigningInput {
        val nonce = extras?.nonce ?: throw BlockchainSdkError.CustomError("Invalid nonce")

        return Ethereum.SigningInput.newBuilder()
            .setChainId(ByteString.copyFrom(chainId.toByteArray()))
            .setNonce(ByteString.copyFrom(nonce.toByteArray()))
            .setDestinationAddress(address = destinationAddress)
            .setFeeParams(fee = fee)
            .setTransaction(coinAmount = coinAmount, extras = extras)
            .build()
    }

    private fun Ethereum.SigningInput.Builder.setDestinationAddress(address: String): Ethereum.SigningInput.Builder {
        return setToAddress(address)
    }

    private fun Ethereum.SigningInput.Builder.setFeeParams(fee: Fee.Ethereum): Ethereum.SigningInput.Builder {
        return when (fee) {
            is Fee.Ethereum.EIP1559 -> {
                this
                    .setTxMode(Ethereum.TransactionMode.Enveloped)
                    .setGasLimit(ByteString.copyFrom(fee.gasLimit.toByteArray()))
                    .setMaxFeePerGas(ByteString.copyFrom(fee.maxFeePerGas.toByteArray()))
                    .setMaxInclusionFeePerGas(ByteString.copyFrom(fee.priorityFee.toByteArray()))
            }
            is Fee.Ethereum.Legacy -> {
                val gasPrice = fee.gasPrice.takeUnless(BigInteger::isZero) ?: fee.calculateGasPrice()

                this
                    .setTxMode(Ethereum.TransactionMode.Legacy)
                    .setGasLimit(ByteString.copyFrom(fee.gasLimit.toByteArray()))
                    .setGasPrice(ByteString.copyFrom(gasPrice.toByteArray()))
            }
        }
    }

    private fun Ethereum.SigningInput.Builder.setFeeParams(
        parsed: EthereumCompiledTransaction,
    ): Ethereum.SigningInput.Builder {
        val maxPriorityFeePerGas = parsed.maxPriorityFeePerGas
        val maxFeePerGas = parsed.maxFeePerGas
        val gasPrice = parsed.gasPrice

        return when {
            maxFeePerGas != null && maxPriorityFeePerGas != null -> {
                this
                    .setTxMode(Ethereum.TransactionMode.Enveloped)
                    .setGasLimit(ByteString.copyFrom(parsed.gasLimit.hexToBytes()))
                    .setMaxFeePerGas(ByteString.copyFrom(maxFeePerGas.hexToBytes()))
                    .setMaxInclusionFeePerGas(ByteString.copyFrom(maxPriorityFeePerGas.hexToBytes()))
            }
            gasPrice != null -> {
                this
                    .setTxMode(Ethereum.TransactionMode.Legacy)
                    .setGasLimit(ByteString.copyFrom(parsed.gasLimit.hexToBytes()))
                    .setGasPrice(ByteString.copyFrom(gasPrice.hexToBytes()))
            }
            else -> error("Transaction fee must be specified")
        }
    }

    private fun Fee.Ethereum.Legacy.calculateGasPrice(): BigInteger {
        val feeValue = amount.value
            ?.movePointRight(amount.decimals)
            ?.toBigInteger()
            ?: error("Transaction fee must be specified")

        return feeValue.divide(gasLimit)
    }

    private fun Ethereum.SigningInput.Builder.setTransaction(
        coinAmount: BigInteger,
        extras: EthereumTransactionExtras?,
    ): Ethereum.SigningInput.Builder {
        return setTransaction(
            Ethereum.Transaction.newBuilder()
                .setContractGeneric(coinAmount = coinAmount, data = extras?.data)
                .build(),
        )
    }

    private fun Ethereum.SigningInput.Builder.setTransaction(
        coinAmount: BigInteger,
        data: ByteArray?,
    ): Ethereum.SigningInput.Builder {
        return setTransaction(
            Ethereum.Transaction.newBuilder()
                .setContractGeneric(coinAmount = coinAmount, data = data)
                .build(),
        )
    }

    private fun Ethereum.Transaction.Builder.setContractGeneric(
        coinAmount: BigInteger,
        data: ByteArray?,
    ): Ethereum.Transaction.Builder {
        return setContractGeneric(
            Ethereum.Transaction.ContractGeneric.newBuilder()
                .setAmount(ByteString.copyFrom(coinAmount.toByteArray()))
                .setInputIfNotNull(data)
                .build(),
        )
    }

    private fun Ethereum.Transaction.ContractGeneric.Builder.setInputIfNotNull(
        data: ByteArray?,
    ): Ethereum.Transaction.ContractGeneric.Builder {
        return if (data != null) setData(ByteString.copyFrom(data)) else this
    }

    private fun buildTxCompilerPreSigningOutput(input: Ethereum.SigningInput): PreSigningOutput {
        val txInputData = input.toByteArray()
        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.CustomError("Error while parse preImageHashes")
        }

        return preSigningOutput
    }

    private fun buildSigningOutput(
        input: Ethereum.SigningInput,
        hash: ByteArray,
        signature: ByteArray,
    ): Ethereum.SigningOutput {
        if (signature.size != SIGNATURE_SIZE) throw BlockchainSdkError.CustomError("Invalid signature size")

        val unmarshalSignature = UnmarshalHelper()
            .unmarshalSignatureExtended(
                signature = signature,
                hash = hash,
                publicKey = decompressedPublicKey,
            )
            .asRSV()

        val txInputData = input.toByteArray()

        val publicKeys = DataVector()
        publicKeys.add(decompressedPublicKey)

        val signatures = DataVector()
        signatures.add(unmarshalSignature)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            txInputData,
            signatures,
            publicKeys,
        )

        val output = Ethereum.SigningOutput.parseFrom(compileWithSignatures)

        if (output.error != Common.SigningError.OK || output.encoded.isEmpty) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return output
    }

    private companion object {
        const val SIGNATURE_SIZE = 64
    }
}