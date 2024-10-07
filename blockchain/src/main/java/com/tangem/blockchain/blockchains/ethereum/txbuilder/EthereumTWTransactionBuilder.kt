package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.toByteArray
import org.kethereum.extensions.toByteArray
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

    override fun buildForSign(transaction: TransactionData): EthereumCompiledTxInfo.TWInfo {
        val input = buildSigningInput(transaction)
        val preSigningOutput = buildTxCompilerPreSigningOutput(input)
        return EthereumCompiledTxInfo.TWInfo(hash = preSigningOutput.dataHash.toByteArray())
    }

    override fun buildForSend(
        transaction: TransactionData,
        signature: ByteArray,
        compiledTransaction: EthereumCompiledTxInfo,
    ): ByteArray {
        val input = buildSigningInput(transaction)
        val output = buildSigningOutput(input = input, hash = compiledTransaction.hash, signature = signature)
        return output.encoded.toByteArray()
    }

    override fun buildDummyTransactionForL1(
        amount: Amount,
        destination: String,
        data: String?,
        fee: Fee.Ethereum,
    ): ByteArray {
        val eip1559Fee = fee as Fee.Ethereum.EIP1559
        val extras = EthereumTransactionExtras(data = data?.toByteArray(), nonce = BigInteger.ONE)

        val input = when (amount.type) {
            AmountType.Coin -> {
                buildSigningInput(
                    destinationType = DestinationType.User(
                        destinationAddress = destination,
                        value = amount.longValueOrZero.toByteArray(),
                    ),
                    fee = eip1559Fee,
                    extras = extras,
                )
            }
            is AmountType.Token -> {
                buildSigningInput(
                    destinationType = DestinationType.Contract(
                        destinationAddress = destination,
                        value = amount.longValueOrZero.toByteArray(),
                        contract = amount.type.token.contractAddress,
                    ),
                    fee = eip1559Fee,
                    extras = extras,
                )
            }
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> error("Invalid amount type: ${amount.type}")
        }

        val preSigningOutput = buildTxCompilerPreSigningOutput(input)
        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(transaction: TransactionData, hash: ByteArray, signature: ByteArray): ByteArray {
        val input = buildSigningInput(transaction)
        val output = buildSigningOutput(input = input, hash = hash, signature = signature)
        return output.encoded.toByteArray()
    }

    private fun buildSigningInput(transaction: TransactionData): Ethereum.SigningInput {
        transaction.requireUncompiled()

        val amountValue = transaction.amount.value?.movePointRight(transaction.amount.decimals)
            ?.toBigInteger()
            ?.toByteArray()
            ?: throw BlockchainSdkError.CustomError("Fail to parse amount")

        val ethereumFee = transaction.fee as? Fee.Ethereum ?: throw BlockchainSdkError.CustomError("Invalid fee")
        val extras = transaction.extras as? EthereumTransactionExtras

        return when (transaction.amount.type) {
            AmountType.Coin -> buildSigningInput(
                destinationType = DestinationType.User(
                    destinationAddress = transaction.destinationAddress,
                    value = amountValue,
                ),
                fee = ethereumFee,
                extras = extras,
            )
            is AmountType.Token -> buildSigningInput(
                destinationType = DestinationType.Contract(
                    destinationAddress = transaction.destinationAddress,
                    contract = transaction.contractAddress ?: transaction.amount.type.token.contractAddress,
                    value = amountValue,
                ),
                fee = transaction.fee,
                extras = extras,
            )
            else -> throw BlockchainSdkError.CustomError("Not implemented")
        }
    }

    private fun buildSigningInput(
        destinationType: DestinationType,
        fee: Fee.Ethereum,
        extras: EthereumTransactionExtras?,
    ): Ethereum.SigningInput {
        val nonce = extras?.nonce ?: throw BlockchainSdkError.CustomError("Invalid nonce")

        return Ethereum.SigningInput.newBuilder()
            .setChainId(ByteString.copyFrom(chainId.toByteArray()))
            .setNonce(ByteString.copyFrom(nonce.toByteArray()))
            .setDestinationAddress(destinationType = destinationType)
            .setFeeParams(fee = fee)
            .setTransaction(destinationType = destinationType, extras = extras)
            .build()
    }

    private fun Ethereum.SigningInput.Builder.setDestinationAddress(
        destinationType: DestinationType,
    ): Ethereum.SigningInput.Builder {
        return setToAddress(
            when (destinationType) {
                is DestinationType.User -> destinationType.destinationAddress
                is DestinationType.Contract -> destinationType.contract
            },
        )
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
                this
                    .setTxMode(Ethereum.TransactionMode.Legacy)
                    .setGasLimit(ByteString.copyFrom(fee.gasLimit.toByteArray()))
                    .setGasPrice(ByteString.copyFrom(fee.gasPrice.toByteArray()))
            }
        }
    }

    private fun Ethereum.SigningInput.Builder.setTransaction(
        destinationType: DestinationType,
        extras: EthereumTransactionExtras?,
    ): Ethereum.SigningInput.Builder {
        return setTransaction(
            Ethereum.Transaction.newBuilder()
                .apply {
                    when (destinationType) {
                        is DestinationType.User -> setTransfer(user = destinationType, extras = extras)
                        is DestinationType.Contract -> {
                            if (extras?.data != null) {
                                setContractGeneric(contract = destinationType, data = extras.data)
                            } else {
                                setErc20Transfer(contract = destinationType)
                            }
                        }
                    }
                }
                .build(),
        )
    }

    private fun Ethereum.Transaction.Builder.setTransfer(
        user: DestinationType.User,
        extras: EthereumTransactionExtras?,
    ): Ethereum.Transaction.Builder {
        val data = extras?.data ?: ByteArray(0)

        return setTransfer(
            Ethereum.Transaction.Transfer.newBuilder()
                .setAmount(ByteString.copyFrom(user.value))
                .setData(ByteString.copyFrom(data))
                .build(),
        )
    }

    private fun Ethereum.Transaction.Builder.setContractGeneric(
        contract: DestinationType.Contract,
        data: ByteArray,
    ): Ethereum.Transaction.Builder {
        return setContractGeneric(
            Ethereum.Transaction.ContractGeneric.newBuilder()
                .setAmount(ByteString.copyFrom(contract.value))
                .setData(ByteString.copyFrom(data))
                .build(),
        )
    }

    private fun Ethereum.Transaction.Builder.setErc20Transfer(
        contract: DestinationType.Contract,
    ): Ethereum.Transaction.Builder {
        return setErc20Transfer(
            Ethereum.Transaction.ERC20Transfer.newBuilder()
                .setAmount(ByteString.copyFrom(contract.value))
                .setTo(contract.destinationAddress)
                .build(),
        )
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

    sealed interface DestinationType {
        val destinationAddress: String
        val value: ByteArray

        data class User(override val destinationAddress: String, override val value: ByteArray) : DestinationType

        data class Contract(
            override val destinationAddress: String,
            override val value: ByteArray,
            val contract: String,
        ) : DestinationType
    }

    private companion object {
        const val SIGNATURE_SIZE = 64
    }
}