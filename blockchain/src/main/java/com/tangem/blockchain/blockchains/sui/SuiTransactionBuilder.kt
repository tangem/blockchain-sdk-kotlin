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
        val amountValue = amount.value
            ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val gasPrice = networkService.getReferenceGasPrice()
            .successOr { return it }

        val coins = getCoinObjects(walletInfo)
        val reducedBalance = getReducedBalanceInMist(walletInfo.totalBalance, amountValue)
        val isPayAll = reducedBalance <= MIN_GAS_BUDGET

        val input = Sui.SigningInput.newBuilder().apply {
            if (isPayAll) {
                payAllSui = buildPayAllObject(coins, destination)
            } else {
                paySui = buildPayObject(coins, destination, amountValue)
            }

            signer = walletAddress
            gasBudget = if (isPayAll) {
                walletInfo.totalBalance.movePointRight(SuiConstants.MIST_SCALE).toLong()
            } else {
                maxOf(MIN_GAS_BUDGET, reducedBalance)
            }
            referenceGasPrice = gasPrice.toLong()
        }.build()

        val signatureMask = ByteArray(size = 64, init = { 0x01 })
        val compiled = TransactionCompiler.compileWithSignatures(
            /* coinType = */ CoinType.SUI,
            /* txInputData = */ input.toByteArray(),
            /* signatures = */ signatureMask.let(::DataVector),
            /* publicKeys = */ publicKey.blockchainKey.let(::DataVector),
        )
        val output = Sui.SigningOutput.parseFrom(compiled)

        return Result.Success(output.unsignedTx)
    }

    suspend fun buildForSend(
        walletInfo: SuiWalletInfo,
        txData: TransactionData,
        txSigner: TransactionSigner,
    ): Result<Sui.SigningOutput> {
        txData.requireUncompiled()

        val suiFee = txData.fee as? Fee.Sui
            ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        val amountValue = txData.amount.value
            ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        val feeValue = suiFee.amount.value
            ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val coins = getCoinObjects(walletInfo)
        val input = Sui.SigningInput.newBuilder().apply {
            if (amountValue + feeValue >= walletInfo.totalBalance) {
                payAllSui = buildPayAllObject(coins, txData.destinationAddress)
            } else {
                paySui = buildPayObject(coins, txData.destinationAddress, amountValue)
            }

            signer = walletAddress
            gasBudget = suiFee.gasBudget.coerceIn(
                range = MIN_GAS_BUDGET..getReducedBalanceInMist(walletInfo.totalBalance, amountValue),
            )
            referenceGasPrice = suiFee.gasPrice
        }.build()

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

    private fun buildPayObject(
        coins: List<Sui.ObjectRef>,
        destinationAddress: String,
        amount: BigDecimal,
    ): Sui.PaySui = Sui.PaySui.newBuilder().apply {
        addAllInputCoins(coins)
        addRecipients(destinationAddress)
        addAmounts(amount.movePointRight(SuiConstants.MIST_SCALE).toLong())
    }.build()

    private fun buildPayAllObject(coins: List<Sui.ObjectRef>, destinationAddress: String): Sui.PayAllSui =
        Sui.PayAllSui.newBuilder().apply {
            addAllInputCoins(coins)
            recipient = destinationAddress
        }.build()

    private fun getCoinObjects(walletInfo: SuiWalletInfo): List<Sui.ObjectRef> {
        return walletInfo.suiCoins.map {
            Sui.ObjectRef.newBuilder().apply {
                objectId = it.objectId
                version = it.version
                objectDigest = it.digest
            }.build()
        }
    }

    private fun getReducedBalanceInMist(totalBalance: BigDecimal, amount: BigDecimal): Long = (totalBalance - amount)
        .movePointRight(SuiConstants.MIST_SCALE)
        .toLong()
        .coerceAtMost(MAX_GAS_BUDGET)

    private companion object {

        const val MIN_GAS_BUDGET = 1_000_000L // 0.01 SUI
        const val MAX_GAS_BUDGET = 50_000_000_000L // 50 SUI
    }
}