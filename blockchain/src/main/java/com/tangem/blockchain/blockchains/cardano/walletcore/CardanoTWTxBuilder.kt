package com.tangem.blockchain.blockchains.cardano.walletcore

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.*
import com.tangem.common.extensions.toByteArray
import wallet.core.jni.Cardano.minAdaAmount
import wallet.core.jni.proto.Cardano
import java.math.BigDecimal

/**
 * Cardano transaction builder using WalletCore
 *
 * @property wallet  wallet
 * @property outputs outputs
 *
[REDACTED_AUTHOR]
 */
internal class CardanoTWTxBuilder(
    private val wallet: Wallet,
    private val outputs: List<CardanoUnspentOutput>,
) {

    /** Build transaction input by [transaction] */
    fun build(transaction: TransactionData): Cardano.SigningInput {
        return Cardano.SigningInput.newBuilder()
            .setTransferMessage(createTransfer(transaction = transaction))
            .setTtl(TRANSACTION_TTL)
            .addAllUtxos(outputs.map(::createTxInput))
            .build()
    }

    /** Calculate required min-ada-value to withdraw all [tokens] */
    fun calculateMinAdaValueToWithdrawAllTokens(tokens: Set<Token>): Long {
        val assets = outputs
            .flatMap(CardanoUnspentOutput::assets)
            .filter { asset ->
                tokens.any { token ->
                    token.contractAddress.startsWith(prefix = asset.policyID)
                }
            }

        val tokenBundle = Cardano.TokenBundle.newBuilder()
            .addAllToken(
                assets.map(::createTokenAmountFromAsset),
            )
            .build()

        return minAdaAmount(tokenBundle.toByteArray())
    }

    private fun createTransfer(transaction: TransactionData): Cardano.Transfer {
        return Cardano.Transfer.newBuilder()
            .setToAddress(transaction.destinationAddress)
            .setChangeAddress(transaction.sourceAddress)
            .setAmountByType(amount = transaction.amount)
            .setUseMaxAmount(false)
            .build()
    }

    private fun Cardano.Transfer.Builder.setAmountByType(amount: Amount): Cardano.Transfer.Builder {
        when (val type = amount.type) {
            is AmountType.Coin -> {
                this.amount = amount.longValueOrZero
            }
            is AmountType.Token -> {
                setTokenAmount(token = type.token, amount = amount.longValueOrZero)
            }
            is AmountType.Reserve -> throw BlockchainSdkError.CustomError("Reserve amount is not supported")
        }

        return this
    }

    private fun Cardano.Transfer.Builder.setTokenAmount(token: Token, amount: Long): Cardano.Transfer.Builder {
        val asset = outputs
            .flatMap(CardanoUnspentOutput::assets)
            .firstOrNull { token.contractAddress.startsWith(prefix = it.policyID) }
            ?: throw BlockchainSdkError.FailedToBuildTx

        val tokenBundle = Cardano.TokenBundle.newBuilder()
            .addToken(createTokenAmount(asset, amount))
            .build()

        val minAdaValue = minAdaAmount(tokenBundle.toByteArray())
        val balance = wallet.getCoinAmount().longValueOrZero
        val remainingBalance = balance - minAdaValue

        val minChange = BigDecimal.ONE.movePointRight(wallet.blockchain.decimals()).toLong()
        val adaAmount = if (remainingBalance in 1 until minChange) {
            balance
        } else {
            minAdaValue
        }

        setAmount(adaAmount)
        tokenAmount = tokenBundle

        return this
    }

    private fun createTokenAmount(asset: CardanoUnspentOutput.Asset, amount: Long): Cardano.TokenAmount {
        return Cardano.TokenAmount.newBuilder()
            .setPolicyId(asset.policyID)
            .setAssetNameHex(asset.assetNameHex)
            .setAmount(ByteString.copyFrom(amount.toByteArray()))
            .build()
    }

    private fun createTxInput(output: CardanoUnspentOutput): Cardano.TxInput {
        return Cardano.TxInput.newBuilder()
            .setOutPoint(
                Cardano.OutPoint.newBuilder()
                    .setTxHash(ByteString.copyFrom(output.transactionHash))
                    .setOutputIndex(output.outputIndex)
                    .build(),
            )
            .setAddress(output.address)
            .setAmount(output.amount)
            .apply {
                if (output.assets.isNotEmpty()) {
                    addAllTokenAmount(output.assets.map(::createTokenAmountFromAsset))
                }
            }
            .build()
    }

    private fun createTokenAmountFromAsset(asset: CardanoUnspentOutput.Asset): Cardano.TokenAmount {
        return Cardano.TokenAmount.newBuilder()
            .setPolicyId(asset.policyID)
            .setAssetNameHex(asset.assetNameHex)
            .setAmount(ByteString.copyFrom(asset.amount.toByteArray()))
            .build()
    }

    private companion object {
        const val TRANSACTION_TTL = 190000000L
    }
}