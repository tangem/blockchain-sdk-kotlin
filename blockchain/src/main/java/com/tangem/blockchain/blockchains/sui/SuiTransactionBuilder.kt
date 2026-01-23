package com.tangem.blockchain.blockchains.sui

import com.tangem.blockchain.blockchains.sui.model.SuiCoin
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.COIN_TYPE
import com.tangem.blockchain.blockchains.sui.network.SuiConstants.SUI_GAS_BUDGET_MAX_VALUE
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.fold
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
) {

    fun buildForDryRun(
        walletInfo: SuiWalletInfo,
        amount: Amount,
        destination: String,
        gasPrice: BigDecimal,
    ): Result<String> {
        return when (amount.type) {
            AmountType.Coin -> buildForInspectCoinTransaction(
                walletInfo = walletInfo,
                amount = amount,
                destination = destination,
                gasPrice = gasPrice,
            )
            is AmountType.Token -> buildForInspectTokenTransaction(
                amount = amount,
                token = amount.type.token,
                destination = destination,
                referenceGasPrice = gasPrice,
                suiWallet = walletInfo,
            ).fold(success = { Result.Success(it) }, failure = {
                val isLessThenOne = isCoinGasLessThenOneForTokenTransaction(walletInfo, amount)
                if (isLessThenOne) Result.Failure(BlockchainSdkError.Sui.OneSuiRequired) else Result.Failure(it)
            })
            else -> Result.Failure(BlockchainSdkError.FailedToBuildTx)
        }
    }

    fun checkOnFailureFeeLoad(walletInfo: SuiWalletInfo, amount: Amount): Result.Failure {
        val isLessThenOne = isCoinGasLessThenOneForTokenTransaction(walletInfo, amount)
        val error = if (isLessThenOne) BlockchainSdkError.Sui.OneSuiRequired else BlockchainSdkError.FailedToLoadFee
        return Result.Failure(error)
    }

    private fun isCoinGasLessThenOneForTokenTransaction(walletInfo: SuiWalletInfo, amount: Amount): Boolean {
        return when (amount.type) {
            is AmountType.Token -> {
                val coinGas = findCoinGas(walletInfo)?.mistBalance ?: return true
                coinGas < BigDecimal.ONE.movePointRight(Blockchain.Sui.decimals())
            }
            else -> false
        }
    }

    private fun buildForInspectCoinTransaction(
        walletInfo: SuiWalletInfo,
        amount: Amount,
        destination: String,
        gasPrice: BigDecimal,
    ): Result<String> {
        val amountMist = amount.value
            ?.movePointRight(Blockchain.Sui.decimals())
            ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val totalBalanceMist = walletInfo.suiTotalBalance.movePointRight(Blockchain.Sui.decimals())
        val isPayAll = amountMist == totalBalanceMist
        val availableBudget = if (isPayAll) {
            totalBalanceMist - BigDecimal.ONE
        } else {
            totalBalanceMist - amountMist
        }
        val budget = minOf(availableBudget, SUI_GAS_BUDGET_MAX_VALUE)

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
        val uncompiledTransaction = txData.requireUncompiled()
        val amount = uncompiledTransaction.amount

        val input = when (amount.type) {
            AmountType.Coin -> buildSigningInputObject(
                walletInfo = walletInfo,
                destinationAddress = uncompiledTransaction.destinationAddress,
                amountMist = amount.value
                    ?.movePointRight(Blockchain.Sui.decimals())
                    ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
                fee = uncompiledTransaction.fee as? Fee.Sui
                    ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
            )
            is AmountType.Token -> makeTokenInput(
                decimalAmount = amount.value?.movePointRight(amount.type.token.decimals)
                    ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
                token = amount.type.token,
                destination = uncompiledTransaction.destinationAddress,
                fee = uncompiledTransaction.fee as? Fee.Sui
                    ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
                suiWallet = walletInfo,
            ) ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)

            is AmountType.TokenYieldSupply,
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        }

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

    private fun buildForInspectTokenTransaction(
        amount: Amount,
        token: Token,
        destination: String,
        referenceGasPrice: BigDecimal,
        suiWallet: SuiWalletInfo,
    ): Result<String> {
        val decimalAmount = amount.value?.movePointRight(token.decimals)
            ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        val availableBudget = suiWallet.coins
            .filter { it.coinType == COIN_TYPE }
            .maxByOrNull { it.mistBalance }
            ?.mistBalance ?: BigDecimal.ZERO
        val budget = minOf(availableBudget, SUI_GAS_BUDGET_MAX_VALUE)

        val input = makeTokenInput(
            decimalAmount = decimalAmount,
            token = token,
            destination = destination,
            suiWallet = suiWallet,
            fee = Fee.Sui(
                gasPrice = referenceGasPrice.toLong(),
                gasBudget = budget.toLong(),
                amount = amount,
            ),
        ) ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val signatureMock = ByteArray(size = 64, init = { 0x01 })

        val compiled = TransactionCompiler.compileWithSignatures(
            /* coinType = */ CoinType.SUI,
            /* txInputData = */ input.toByteArray(),
            /* signatures = */ signatureMock.let(::DataVector),
            /* publicKeys = */ publicKey.blockchainKey.let(::DataVector),
        )

        val output = Sui.SigningOutput.parseFrom(compiled)
        return Result.Success(output.unsignedTx)
    }

    private fun makeTokenInput(
        decimalAmount: BigDecimal,
        token: Token,
        destination: String,
        fee: Fee.Sui,
        suiWallet: SuiWalletInfo,
    ): Sui.SigningInput? {
        val coinToUse = getCoins(decimalAmount, token, suiWallet)

        val coinGas = findCoinGas(suiWallet) ?: return null

        return Sui.SigningInput.newBuilder().apply {
            val inputCoins = coinToUse.map { coin ->
                Sui.ObjectRef.newBuilder().apply {
                    version = coin.version
                    objectId = coin.objectId
                    objectDigest = coin.digest
                }.build()
            }

            pay = Sui.Pay.newBuilder().apply {
                addAllInputCoins(inputCoins)
                addRecipients(destination)
                addAmounts(decimalAmount.toBigInteger().toLong())
                gas = Sui.ObjectRef.newBuilder().apply {
                    version = coinGas.version
                    objectId = coinGas.objectId
                    objectDigest = coinGas.digest
                }.build()
            }.build()

            signer = walletAddress
            gasBudget = fee.gasBudget.toBigInteger().toLong()
            referenceGasPrice = fee.gasPrice.toBigInteger().toLong()
        }.build()
    }

    private fun findCoinGas(suiWallet: SuiWalletInfo): SuiCoin? = suiWallet.coins
        .filter { it.coinType == COIN_TYPE }
        .maxByOrNull { it.mistBalance }

    private fun getCoins(amount: BigDecimal, token: Token, suiWallet: SuiWalletInfo): List<SuiCoin> {
        val inputs = mutableListOf<SuiCoin>()
        var total = BigDecimal.ZERO
        val tokenObjects = suiWallet.coins.filter { it.coinType == token.contractAddress }

        for (coin in tokenObjects) {
            inputs.add(coin)
            total += coin.mistBalance

            if (total >= amount) {
                break
            }
        }

        return inputs
    }
}