package com.tangem.blockchain.blockchains.cosmos

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.proto.CosmosProtoMessage
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.toCompressedPublicKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.Cosmos
import wallet.core.jni.proto.Cosmos.SigningOutput
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

@Suppress("LongParameterList")
internal class CosmosTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
    private val cosmosChain: CosmosChain,
) {

    fun buildForSign(
        amount: Amount,
        source: String,
        destination: String,
        accountNumber: Long,
        sequenceNumber: Long,
        feeAmount: Amount?,
        gas: Long?,
        extras: CosmosTransactionExtras?,
    ): ByteArray {
        val txInputData = makeInput(
            publicKey = publicKey,
            amount = amount,
            source = source,
            destination = destination,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = feeAmount,
            gas = gas,
            extras = extras,
        ).toByteArray()

        return buildForSignInternal(txInputData)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun buildForSign(accountNumber: Long, sequenceNumber: Long, compiledTransaction: ByteArray): ByteArray {
        val serialized = ProtoBuf.decodeFromByteArray<CosmosProtoMessage>(compiledTransaction)
        val txInputData = makeInput(
            protoMessage = serialized,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
        ).toByteArray()

        return buildForSignInternal(txInputData)
    }

    fun buildForSend(
        amount: Amount,
        source: String,
        destination: String,
        accountNumber: Long,
        sequenceNumber: Long,
        feeAmount: Amount?,
        gas: Long?,
        extras: CosmosTransactionExtras?,
        signature: ByteArray,
    ): String {
        val txInputData = makeInput(
            publicKey = publicKey,
            amount = amount,
            source = source,
            destination = destination,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = feeAmount,
            gas = gas,
            extras = extras,
        ).toByteArray()

        return buildForSignInternal(txInputData, signature)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun buildForSend(
        compiledTransaction: ByteArray,
        accountNumber: Long,
        sequenceNumber: Long,
        signature: ByteArray,
    ): String {
        val serialized = ProtoBuf.decodeFromByteArray<CosmosProtoMessage>(compiledTransaction)
        val txInputData = makeInput(
            protoMessage = serialized,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
        ).toByteArray()

        return buildForSignInternal(txInputData, signature)
    }

    private fun makeInput(
        publicKey: Wallet.PublicKey,
        amount: Amount,
        source: String,
        destination: String,
        accountNumber: Long,
        sequenceNumber: Long,
        feeAmount: Amount?,
        gas: Long?,
        extras: CosmosTransactionExtras?,
    ): Cosmos.SigningInput {
        val decimalValue = (amount.type as? AmountType.Token)?.token?.decimals ?: cosmosChain.blockchain.decimals()
        val amountInSmallestDenomination = amount.value?.movePointRight(decimalValue)?.toLong() ?: 0
        val denomination = denomination(amount)
        val sendCoinsMessage = Cosmos.Message.Send.newBuilder()
            .setFromAddress(source)
            .setToAddress(destination)
            .addAmounts(
                Cosmos.Amount.newBuilder()
                    .setAmount(amountInSmallestDenomination.toString())
                    .setDenom(denomination),
            )
            .build()
        val message = Cosmos.Message.newBuilder()
            .setSendCoinsMessage(sendCoinsMessage)
            .build()
        val fee = if (feeAmount != null && gas != null) {
            val feeInSmallestDenomination = feeAmount.value?.movePointRight(decimalValue)?.toLong() ?: 0
            Cosmos.Fee.newBuilder()
                .setGas(gas)
                .addAmounts(
                    Cosmos.Amount.newBuilder()
                        .setAmount(feeInSmallestDenomination.toString())
                        .setDenom(denomination),
                )
        } else {
            null
        }

        @Suppress("MagicNumber")
        val input = Cosmos.SigningInput.newBuilder()
            .setMode(Cosmos.BroadcastMode.SYNC)
            .setSigningMode(Cosmos.SigningMode.Protobuf)
            .setAccountNumber(accountNumber)
            .setChainId(cosmosChain.chainId)
            .setMemo(extras?.memo ?: "")
            .setSequence(sequenceNumber)
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey.toCompressedPublicKey()))
            .addMessages(message)
            .setPrivateKey(ByteString.copyFrom(ByteArray(32) { 1 }))

        if (fee != null) {
            input.setFee(fee)
        }

        return input.build()
    }

    private fun makeInput(
        protoMessage: CosmosProtoMessage,
        accountNumber: Long,
        sequenceNumber: Long,
    ): Cosmos.SigningInput {
        val delegatorMessage = protoMessage.delegateContainer.delegate.delegateData
        val amountMessage = delegatorMessage.delegateAmount
        val feeMessage = protoMessage.feeAndKeyContainer.feeContainer
        val feeValue = feeMessage.feeAmount

        val sendCoinsMessage = Cosmos.Message.Delegate.newBuilder()
            .setAmount(
                Cosmos.Amount.newBuilder()
                    .setAmount(amountMessage.amount)
                    .setDenom(amountMessage.denomination),
            )
            .setDelegatorAddress(delegatorMessage.delegatorAddress)
            .setValidatorAddress(delegatorMessage.validatorAddress)
            .build()
        val message = Cosmos.Message.newBuilder()
            .setStakeMessage(sendCoinsMessage)
            .build()
        val fee = Cosmos.Fee.newBuilder()
            .setGas(feeMessage.gas)
            .addAmounts(
                Cosmos.Amount.newBuilder()
                    .setAmount(feeValue.amount)
                    .setDenom(feeValue.denomination),
            )

        @Suppress("MagicNumber")
        val input = Cosmos.SigningInput.newBuilder()
            .setMode(Cosmos.BroadcastMode.SYNC)
            .setSigningMode(Cosmos.SigningMode.Protobuf)
            .setAccountNumber(accountNumber)
            .setChainId(cosmosChain.chainId)
            .setSequence(sequenceNumber)
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey.toCompressedPublicKey()))
            .addMessages(message)
            .setPrivateKey(ByteString.copyFrom(ByteArray(32) { 1 }))
            .setFee(fee)

        return input.build()
    }

    private fun buildForSignInternal(txInputData: ByteArray): ByteArray {
        val preImageHashes = TransactionCompiler.preImageHashes(cosmosChain.coin, txInputData)
        val output = PreSigningOutput.parseFrom(preImageHashes)

        if (output.error != Common.SigningError.OK) {
            throw BlockchainSdkError.CustomError("Error while parse preImageHashes")
        }

        return output.dataHash.toByteArray()
    }

    private fun buildForSignInternal(txInputData: ByteArray, signature: ByteArray): String {
        // compressed key because old cards have 65 bytes PK, new cards have 33 bytes
        // wallet core requires 33 bytes
        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey.toCompressedPublicKey())

        val signatures = DataVector()
        signatures.add(signature)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            cosmosChain.coin,
            txInputData,
            signatures,
            publicKeys,
        )

        // transaction compiled with signatures may contain garbage bytes before json, we need drop them
        val output = SigningOutput.parseFrom(compileWithSignatures)

        if (output.error != Common.SigningError.OK) {
            throw BlockchainSdkError.CustomError(output.errorMessage)
        }

        return output.serialized
    }

    private fun denomination(amount: Amount): String {
        return when (amount.type) {
            AmountType.Coin -> cosmosChain.smallestDenomination
            is AmountType.Token -> {
                cosmosChain.tokenDenominationByContractAddress[amount.type.token.contractAddress]
                    ?: throw BlockchainSdkError.FailedToBuildTx
            }

            else -> throw BlockchainSdkError.FailedToBuildTx
        }
    }
}