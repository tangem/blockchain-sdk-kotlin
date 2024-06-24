package com.tangem.blockchain.blockchains.cardano.walletcore

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.di.DepsContainer
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
        if (transaction.amount.type is AmountType.Token &&
            !DepsContainer.blockchainFeatureToggles.isCardanoTokenSupport
        ) {
            throw BlockchainSdkError.CustomError("Cardano tokens isn't supported")
        }

        return Cardano.SigningInput.newBuilder()
            .setTransferMessage(createTransfer(transaction = transaction))
            .setTtl(TRANSACTION_TTL)
            .addAllUtxos(outputs.map(::createTxInput))
            .build()
    }

    /** Calculate required min-ada-value to withdraw all tokens */
    fun calculateMinAdaValueToWithdrawAllTokens(tokens: Map<Cardano.TokenAmount, Long>): Long {
        if (!DepsContainer.blockchainFeatureToggles.isCardanoTokenSupport) {
            throw BlockchainSdkError.CustomError("Cardano tokens isn't supported")
        }

        val tokenBundle = Cardano.TokenBundle.newBuilder()
            .addAllToken(
                tokens.map {
                    val token = it.key

                    Cardano.TokenAmount.newBuilder()
                        .setPolicyId(token.policyId)
                        .setAssetNameHex(token.assetNameHex)
                        .setAmount(ByteString.copyFrom(it.value.toByteArray()))
                        .build()
                },
            )
            .build()

        return minAdaAmount(tokenBundle.toByteArray())
    }

    /** Calculate required min-ada-value to withdraw [amount] token with [contractAddress] */
    fun calculateMinAdaValueToWithdrawToken(contractAddress: String, amount: Long): Long {
        if (!DepsContainer.blockchainFeatureToggles.isCardanoTokenSupport) {
            throw BlockchainSdkError.CustomError("Cardano tokens isn't supported")
        }

        val tokenBundle = createTokenBundle(contractAddress = contractAddress, amount = amount)

        return minAdaAmount(tokenBundle.toByteArray())
    }

    private fun createTransfer(transaction: TransactionData): Cardano.Transfer {
        return Cardano.Transfer.newBuilder()
            .setToAddress(transaction.destinationAddress)
            .setChangeAddress(transaction.sourceAddress)
            .setAmountByType(transaction = transaction)
            .setUseMaxAmount(false)
            .build()
    }

    private fun Cardano.Transfer.Builder.setAmountByType(transaction: TransactionData): Cardano.Transfer.Builder {
        when (val type = transaction.amount.type) {
            is AmountType.Coin -> {
                this.amount = transaction.amount.longValueOrZero
            }
            is AmountType.Token -> {
                setTokenAmount(
                    contractAddress = type.token.contractAddress,
                    amount = transaction.amount.longValueOrZero,
                    fee = transaction.fee?.amount?.longValueOrZero ?: 0,
                )
            }
            else -> throw BlockchainSdkError.CustomError("AmountType $type is not supported")
        }

        return this
    }

    private fun Cardano.Transfer.Builder.setTokenAmount(
        contractAddress: String,
        amount: Long,
        fee: Long,
    ): Cardano.Transfer.Builder {
        val tokenBundle = createTokenBundle(contractAddress = contractAddress, amount = amount)

        val minAdaValue = minAdaAmount(tokenBundle.toByteArray())
        val balance = wallet.getCoinAmount().longValueOrZero

        val remainingBalance = balance - minAdaValue - fee

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

    private fun createTokenBundle(contractAddress: String, amount: Long): Cardano.TokenBundle {
        val asset = outputs
            .flatMap(CardanoUnspentOutput::assets)
            .firstOrNull { contractAddress.startsWith(prefix = it.policyID) }
            ?: throw BlockchainSdkError.FailedToBuildTx

        return Cardano.TokenBundle.newBuilder()
            .addToken(createTokenAmount(asset, amount))
            .build()
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