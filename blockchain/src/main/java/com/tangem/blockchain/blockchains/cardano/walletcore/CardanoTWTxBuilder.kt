package com.tangem.blockchain.blockchains.cardano.walletcore

import co.nstant.`in`.cbor.CborDecoder
import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cardano.converter.CardanoCompiledTransactionConverter
import com.tangem.blockchain.blockchains.cardano.models.CardanoCompiledTransaction
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.utils.matchesCardanoAsset
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.toByteArray
import io.ktor.util.*
import org.ton.tl.ByteString.Companion.decodeFromHex
import wallet.core.jni.Cardano.minAdaAmount
import wallet.core.jni.proto.Cardano
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import wallet.core.jni.Cardano as CardanoExt

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

    /** Build transaction input by [transactionData] */
    fun build(transactionData: TransactionData): Cardano.SigningInput = when (transactionData) {
        is TransactionData.Compiled -> buildCompiled(transactionData)
        is TransactionData.Uncompiled -> buildUncompiled(transactionData)
    }

    private fun buildUncompiled(transactionData: TransactionData): Cardano.SigningInput {
        transactionData.requireUncompiled()

        return Cardano.SigningInput.newBuilder()
            .setTransferMessage(createTransfer(transactionData = transactionData))
            .setTtl(TRANSACTION_TTL)
            .addAllUtxos(outputs.map(::createTxInput))
            .build()
    }

    private fun buildCompiled(transactionData: TransactionData): Cardano.SigningInput {
        transactionData.requireCompiled()
        val transaction = transactionData.value as? TransactionData.Compiled.Data.RawString
            ?: error("Compiled transaction must be in string format")

        val byteData = transaction.data.decodeFromHex().toByteArray()
        val bais = ByteArrayInputStream(byteData)
        val decodedData = CborDecoder(bais)
        val converter = CardanoCompiledTransactionConverter()
        val data = converter.convert(decodedData) ?: error("Failed to parse compiled transaction")
        val stakingAddress = CardanoExt.getStakingAddress(wallet.address)

        val signinInputBuilder = with(Cardano.SigningInput.newBuilder()) {
            addAllUtxos(outputs.map(::createTxInput))
            setTransferMessage(
                Cardano.Transfer.newBuilder()
                    .setToAddress(wallet.address)
                    .setChangeAddress(wallet.address)
                    .setUseMaxAmount(true)
                    .build(),
            )

            data.certificates.forEach { certificate ->
                when (certificate) {
                    is CardanoCompiledTransaction.Certificate.StakeDelegation -> {
                        setDelegate(
                            Cardano.Delegate.newBuilder()
                                .setDepositAmount(0)
                                .setPoolId(ByteString.copyFrom(certificate.poolKeyHash))
                                .setStakingAddress(stakingAddress),
                        )
                    }
                    is CardanoCompiledTransaction.Certificate.StakeDeregistrationLegacy,
                    is CardanoCompiledTransaction.Certificate.StakeDeregistrationConway,
                    -> {
                        setDeregisterStakingKey(
                            Cardano.DeregisterStakingKey.newBuilder()
                                .setStakingAddress(stakingAddress)
                                .setUndepositAmount(STAKING_DEPOSIT_AMOUNT),
                        )
                    }
                    is CardanoCompiledTransaction.Certificate.StakeRegistrationLegacy -> {
                        setRegisterStakingKey(
                            Cardano.RegisterStakingKey.newBuilder()
                                .setStakingAddress(stakingAddress)
                                .setDepositAmount(STAKING_DEPOSIT_AMOUNT),
                        )
                    }
                }
            }

            if (!data.withdrawals.isNullOrEmpty()) {
                setWithdraw(
                    Cardano.Withdraw.newBuilder()
                        .setStakingAddress(stakingAddress)
                        .setWithdrawAmount(data.withdrawals.sum()),
                )
            }

            setTtl(TRANSACTION_TTL)
        }

        return signinInputBuilder.build()
    }

    /** Calculate required min-ada-value to withdraw all tokens */
    fun calculateMinAdaValueToWithdrawAllTokens(tokens: Map<Cardano.TokenAmount, Long>): Long {
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
        val tokenBundle = createTokenBundle(contractAddress = contractAddress, amount = amount)

        return minAdaAmount(tokenBundle.toByteArray())
    }

    private fun createTransfer(transactionData: TransactionData): Cardano.Transfer {
        transactionData.requireUncompiled()

        return Cardano.Transfer.newBuilder()
            .setToAddress(transactionData.destinationAddress)
            .setChangeAddress(transactionData.sourceAddress)
            .setAmountByType(transactionData = transactionData)
            .setUseMaxAmount(false)
            .build()
    }

    private fun Cardano.Transfer.Builder.setAmountByType(transactionData: TransactionData): Cardano.Transfer.Builder {
        transactionData.requireUncompiled()

        when (val type = transactionData.amount.type) {
            is AmountType.Coin -> {
                this.amount = transactionData.amount.longValue
            }
            is AmountType.Token -> {
                setTokenAmount(
                    contractAddress = type.token.contractAddress,
                    amount = transactionData.amount.longValue,
                    fee = transactionData.fee?.amount?.longValue ?: 0L,
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
        val balance = wallet.getCoinAmount().longValue

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
            .firstOrNull {
                contractAddress.matchesCardanoAsset(policyId = it.policyID, assetNameHex = it.assetNameHex)
            }
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
        // Transaction validity time. Currently we are using absolute values.
        // At 16 April 2023 was 90007700 slot number.
        // We need to rework this logic to use relative validity time.
        // TODO: [REDACTED_JIRA]
        // This can be constructed using absolute ttl slot from `/metadata` endpoint.
        const val TRANSACTION_TTL = 190000000L
        const val STAKING_DEPOSIT_AMOUNT = 2000000L
    }
}