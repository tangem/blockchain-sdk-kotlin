package com.tangem.blockchain.blockchains.cosmos

import android.util.Log
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosNetworkService
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import wallet.core.jni.PublicKeyType
import wallet.core.jni.proto.Cosmos
import java.math.RoundingMode

class CosmosWalletManager(
    wallet: Wallet,
    networkProviders: List<CosmosRestProvider>,
    private val cosmosChain: CosmosChain,
) : WalletManager(wallet), TransactionSender {

    private val networkService: CosmosNetworkService = CosmosNetworkService(networkProviders, cosmosChain)
    private var accountNumber: Long? = null
    private var sequenceNumber: Long = 0L
    private val txBuilder: CosmosTransactionBuilder = CosmosTransactionBuilder(cosmosChain = cosmosChain)
    private var gas: Long? = null

    override val currentHost: String
        get() = networkService.host

    override val allowsFeeSelection: FeeSelectionState = cosmosChain.allowsFeeSelection
    override suspend fun update() {
        val unconfirmedTxHashes = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .mapNotNull { it.hash }
        when (val accountResult = networkService.getAccountInfo(wallet.address, cardTokens, unconfirmedTxHashes)) {
            is Result.Failure -> updateError(accountResult.error)
            is Result.Success -> updateWallet(accountResult.data)
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val accNumber = accountNumber ?: return SimpleResult.Failure(BlockchainSdkError.AccountNotFound)
        val input = txBuilder.buildForSign(
            amount = transactionData.amount,
            source = wallet.address,
            destination = transactionData.destinationAddress,
            accountNumber = accNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = transactionData.fee,
            gas = gas,
            extras = transactionData.extras as? CosmosTransactionExtras,
        )

        val message = buildTransaction(input, signer).successOr {
            return SimpleResult.Failure(it.error)
        }
        return when (val sendResult = networkService.send(message)) {
            is Result.Failure -> SimpleResult.Failure(sendResult.error)
            is Result.Success -> {
                val transaction = transactionData.copy(
                    hash = sendResult.data,
                    status = TransactionStatus.Unconfirmed,
                    sourceAddress = wallet.address,
                )
                wallet.recentTransactions.add(transaction)
                SimpleResult.Success
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val accNumber = accountNumber ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        val input = txBuilder.buildForSign(
            amount = amount,
            source = wallet.address,
            destination = destination,
            accountNumber = accNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = null,
            gas = null,
            extras = null,
        )
        val message = buildTransaction(input, null).successOr { return it }
        return when (val estimateGasResult = networkService.estimateGas(message)) {
            is Result.Failure -> estimateGasResult
            is Result.Success -> {
                val amounts = cosmosChain.gasPrices(amount.type).map { gasPrice ->
                    val estimatedGas = estimateGasResult.data
                    val gasMultiplier = cosmosChain.gasMultiplier
                    val feeMultiplier = cosmosChain.feeMultiplier
                    val gas = estimatedGas * gasMultiplier
                    this.gas = gas
                    var feeValueInSmallestDenomination = gas * gasPrice * feeMultiplier
                    tax(amount)?.let { feeValueInSmallestDenomination += it }

                    val value = (feeValueInSmallestDenomination)
                        .toBigDecimal()
                        .movePointLeft(wallet.blockchain.decimals())
                        .setScale(wallet.blockchain.decimals(), RoundingMode.DOWN)
                    Amount(value = value, blockchain = wallet.blockchain)
                }


                if (amounts.size == 3) {
                    return Result.Success(TransactionFee.SetOfThree(amounts[0], amounts[1], amounts[2]))
                } else {
                    return Result.Success(TransactionFee.Single(amounts[0]))
                }

            }
        }
    }

    private fun updateWallet(cosmosAccountInfo: CosmosAccountInfo) {
        wallet.recentTransactions.forEach {
            if (cosmosAccountInfo.confirmedTransactionHashes.contains(it.hash)) it.status = TransactionStatus.Confirmed
        }
        wallet.setAmount(cosmosAccountInfo.amount)
        accountNumber = cosmosAccountInfo.accountNumber
        sequenceNumber = cosmosAccountInfo.sequenceNumber
        cosmosAccountInfo.tokenBalances.forEach { entry ->
            val value = requireNotNull(entry.value.value)
            val token = entry.key
            wallet.addTokenValue(value = value, token = token)
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    private fun buildTransaction(input: Cosmos.SigningInput, signer: TransactionSigner?): Result<String> {
        val outputResult: Result<Cosmos.SigningOutput> = AnySignerWrapper().sign(
            walletPublicKey = wallet.publicKey,
            publicKeyType = PublicKeyType.SECP256K1,
            input = input,
            coin = cosmosChain.coin,
            parser = Cosmos.SigningOutput.parser(),
            signer = signer,
            curve = wallet.blockchain.getSupportedCurves().first(),
        )
        return when (outputResult) {
            is Result.Failure -> outputResult
            is Result.Success -> Result.Success(txBuilder.buildForSend(outputResult.data))
        }
    }

    private fun tax(amount: Amount): Long? {
        return when (amount.type) {
            is AmountType.Token -> {
                val taxPercent =
                    cosmosChain.taxPercentByContractAddress[amount.type.token.contractAddress] ?: return null
                val amountInSmallestDenomination = requireNotNull(amount.longValue) { "Amount must not be null" }
                return amountInSmallestDenomination * taxPercent.toLong() / 100
            }
            else -> null
        }
    }
}