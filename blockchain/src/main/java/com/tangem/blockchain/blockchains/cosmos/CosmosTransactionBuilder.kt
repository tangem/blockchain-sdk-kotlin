package com.tangem.blockchain.blockchains.cosmos

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.crypto.CryptoUtils
import wallet.core.jni.proto.Cosmos

internal class CosmosTransactionBuilder(
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
    ) : Cosmos.SigningInput {
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
            .addMessages(message)
            .setPrivateKey(
                ByteString.copyFrom(
                    CryptoUtils.generateRandomBytes(length = 32)
                )
            )

        if (fee != null) {
            input.setFee(fee)
        }

        return input.build()
    }

    fun buildForSend(output: Cosmos.SigningOutput): String {
        return output.serialized
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