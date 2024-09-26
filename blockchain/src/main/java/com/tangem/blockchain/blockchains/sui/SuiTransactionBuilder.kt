package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.blockchains.sui.network.SuiNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.Sui
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.math.BigDecimal

internal class SuiTransactionBuilder(
    private val walletAddress: String,
    private val publicKey: Wallet.PublicKey,
    private val networkService: SuiNetworkService,
) {

    suspend fun buildForDryRun(walletInfo: SuiWalletInfo, amount: Amount, destination: String): Result<String> {
        val amountMist = amount.value
            ?.movePointRight(SuiConstants.MIST_SCALE)
            ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        val gasPrice = networkService.getReferenceGasPrice()
            .successOr { return it }

        val totalBalanceMist = walletInfo.suiTotalBalance.movePointRight(SuiConstants.MIST_SCALE)
        val isPayAll = amountMist == totalBalanceMist
        val availableBudget = if (isPayAll) {
            totalBalanceMist - BigDecimal.ONE
        } else {
            totalBalanceMist - amountMist
        }
        val budget = minOf(availableBudget, BigDecimal.valueOf(MAX_GAS_BUDGET))

        val input = buildSigningInputObject(
            walletInfo = walletInfo,
            destinationAddress = destination,
            amountMist = if (isPayAll) {
                BigDecimal.ONE
            } else {
                amountMist
            },
            fee = Fee.Sui(
                amount = Amount(Blockchain.Sui),
                gasPrice = gasPrice.toLong(),
                gasBudget = budget.toLong(),
            ),
        )

        val signatureMask = ByteArray(size = 64, init = { 0x01 })
        val compiled = TransactionCompiler.compileWithSignatures(
            /* coinType = */ CoinType.SUI,
            /* txInputData = */ input.toByteArray(),
            /* signatures = */ signatureMask.let(::DataVector),
            /* publicKeys = */ publicKey.blockchainKey.let(::DataVector),
        )

        val compiledOutput = Sui.SigningOutput.parseFrom(compiled)
        if (compiledOutput.error != Common.SigningError.OK) {
            return Result.Failure(
                error = BlockchainSdkError.CustomError("Error while parse compiled dry transaction"),
            )
        }

        return Result.Success(compiledOutput.unsignedTx)
    }

    suspend fun buildForSend(
        walletInfo: SuiWalletInfo,
        txData: TransactionData,
        txSigner: TransactionSigner,
    ): Result<Sui.SigningOutput> {
        txData.requireUncompiled()

        val input = buildSigningInputObject(
            walletInfo = walletInfo,
            destinationAddress = txData.destinationAddress,
            amountMist = txData.amount.value
                ?.movePointRight(SuiConstants.MIST_SCALE)
                ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
            fee = txData.fee as? Fee.Sui
                ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
        )

        val preImageHashes = TransactionCompiler.preImageHashes(
            /* coinType = */ CoinType.SUI,
            /* txInputData = */ input.toByteArray(),
        )
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)
        if (preSigningOutput.error != Common.SigningError.OK) {
            return Result.Failure(
                error = BlockchainSdkError.CustomError("Error while parse preImageHashes"),
            )
        }

        val signature = txSigner.sign(preSigningOutput.dataHash.toByteArray(), publicKey)
            .successOr { return Result.Failure(it.error.toBlockchainSdkError()) }
        val compiled = TransactionCompiler.compileWithSignatures(
            /* coinType = */ CoinType.SUI,
            /* txInputData = */ input.toByteArray(),
            /* signatures = */ signature.let(::DataVector),
            /* publicKeys = */ publicKey.blockchainKey.let(::DataVector),
        )

        val compiledOutput = Sui.SigningOutput.parseFrom(compiled)
        if (compiledOutput.error != Common.SigningError.OK) {
            return Result.Failure(
                error = BlockchainSdkError.CustomError("Error while parse compiled transaction"),
            )
        }

        return Result.Success(compiledOutput)
    }

    private fun buildSigningInputObject(
        walletInfo: SuiWalletInfo,
        destinationAddress: String,
        amountMist: BigDecimal,
        fee: Fee.Sui,
    ): Sui.SigningInput = Sui.SigningInput.newBuilder().apply {
        val coins = getCoinsForAmount(
            walletInfo = walletInfo,
            amountMist = amountMist + fee.gasBudget.toBigDecimal(),
        )

        paySui = Sui.PaySui.newBuilder().apply {
            addAllInputCoins(coins)
            addRecipients(destinationAddress)
            addAmounts(amountMist.toLong())
        }.build()

        signer = walletAddress
        gasBudget = fee.gasBudget
        referenceGasPrice = fee.gasPrice
    }.build()

    private fun getCoinsForAmount(walletInfo: SuiWalletInfo, amountMist: BigDecimal): List<Sui.ObjectRef> {
        val coins = mutableListOf<Sui.ObjectRef>()
        var coinsBalance = BigDecimal.ZERO

        walletInfo.coins.forEach { coin ->
            val coinObject = Sui.ObjectRef.newBuilder().apply {
                objectId = coin.objectId
                version = coin.version
                objectDigest = coin.digest
            }.build()

            coins.add(coinObject)
            coinsBalance += coin.mistBalance

            if (coinsBalance >= amountMist) {
                return coins
            }
        }

        return coins
    }

    private companion object {

        const val MAX_GAS_BUDGET = 50_000_000_000L // 50 SUI
    }
}