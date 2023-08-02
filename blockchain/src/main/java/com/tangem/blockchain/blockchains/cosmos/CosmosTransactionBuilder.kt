package com.tangem.blockchain.blockchains.cosmos

import android.util.Log
import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.*
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.Cosmos
import wallet.core.jni.proto.Cosmos.SigningOutput
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.lang.IllegalStateException

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
        val input =
            makeInput(
                publicKey = publicKey,
                amount = amount,
                source = source,
                destination = destination,
                accountNumber = accountNumber,
                sequenceNumber = sequenceNumber,
                feeAmount = feeAmount,
                gas = gas,
                extras = extras
            )

        Log.e("params:", "publicKey = $publicKey,\n" +
            "amount = $amount,\n" +
            "source = $source,\n" +
            "destination = $destination,\n" +
            "accountNumber = $accountNumber,\n" +
            "sequenceNumber = $sequenceNumber,\n" +
            "feeAmount = $feeAmount,\n" +
            "gas = $gas,\n" +
            "extras = $extras")
        val txInputData = input.toByteArray()
        Log.e("input", input.toString())

        val preImageHashes = TransactionCompiler.preImageHashes(cosmosChain.coin, txInputData)
        Log.e("preImageHashes", preImageHashes.decodeToString())

        val output = PreSigningOutput.parseFrom(preImageHashes)

        if (output.error != Common.SigningError.OK) {
            throw IllegalStateException("something went wrong")
        }

        Log.e("output.dataHash", output.dataHash.toString())
        return output.dataHash.toByteArray()
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
        Log.e("params:", "publicKey = $publicKey,\n" +
            "amount = $amount,\n" +
            "source = $source,\n" +
            "destination = $destination,\n" +
            "accountNumber = $accountNumber,\n" +
            "sequenceNumber = $sequenceNumber,\n" +
            "feeAmount = $feeAmount,\n" +
            "gas = $gas,\n" +
            "extras = $extras\n" +
            "signature = ${signature.map { it }}")

        val input = makeInput(
            publicKey = publicKey,
            amount = amount,
            source = source,
            destination = destination,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = feeAmount,
            gas = gas,
            extras = extras
        )

        val txInputData = input.toByteArray()
        Log.e("txInputData", txInputData.toString())

        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey)

        val signatures = DataVector()
        signatures.add(signature)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            cosmosChain.coin, txInputData, signatures, publicKeys
        )

        Log.e("compileWithSignatures", compileWithSignatures.decodeToString().dropWhile { it != '{' })

        val output = SigningOutput.newBuilder()
            .setSerialized(compileWithSignatures.decodeToString().dropWhile { it != '{' })
            .build()

        if (output.error != Common.SigningError.OK) {
            throw IllegalStateException("something went wrong")
        }

        Log.e("output.serialized", output.serialized)
        return output.serialized
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
                    .setDenom(denomination)
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
                        .setDenom(denomination)
                )
        } else {
            null
        }
        val input = Cosmos.SigningInput.newBuilder()
            .setMode(Cosmos.BroadcastMode.SYNC)
            .setSigningMode(Cosmos.SigningMode.Protobuf)
            .setAccountNumber(accountNumber)
            .setChainId(cosmosChain.chainId)
            .setMemo(extras?.memo ?: "")
            .setSequence(sequenceNumber)
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey))
            .addMessages(message)
            .setPrivateKey(ByteString.copyFrom(ByteArray(32) { 1 }))

        if (fee != null) {
            input.setFee(fee)
        }

        return input.build()
    }

    private fun denomination(amount: Amount): String {
        return when (amount.type) {
            AmountType.Coin -> cosmosChain.smallestDenomination
            is AmountType.Token -> {
                cosmosChain.tokenDenominationByContractAddress[amount.type.token.contractAddress]
                    ?: throw BlockchainSdkError.FailedToBuildTx
            }

            AmountType.Reserve -> throw BlockchainSdkError.FailedToBuildTx
        }
    }
}
