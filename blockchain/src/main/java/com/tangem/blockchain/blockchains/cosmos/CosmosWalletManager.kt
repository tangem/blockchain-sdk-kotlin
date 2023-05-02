package com.tangem.blockchain.blockchains.cosmos

import android.util.Log
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.blockchains.cosmos.network.CosmosNetworkService
import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AnySignerWrapper
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import wallet.core.jni.CoinType
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

    override suspend fun update() {
        when (val accountResult = networkService.getAccountInfo(wallet.address)) {
            is Result.Failure -> {
                Log.e(this::class.java.simpleName, accountResult.error.message, accountResult.error)
                wallet.amounts.clear()
            }
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

        val message = buildTransaction(input, signer).successOr { return SimpleResult.Failure(it.error) }
        return when (val sendResult = networkService.send(message)) {
            is Result.Failure -> SimpleResult.Failure(sendResult.error)
            is Result.Success -> {
                transactionData.hash = sendResult.data
                transactionData.status = TransactionStatus.Confirmed
                wallet.recentTransactions.add(transactionData)
                SimpleResult.Success
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
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
            is Result.Success -> Result.Success(
                cosmosChain.gasPrices.map { gasPrice ->
                    val cosmosGasMultiplier = 2
                    val gas = estimateGasResult.data * cosmosGasMultiplier
                    this.gas = gas
                    val value = (gas * gasPrice)
                        .toBigDecimal()
                        .movePointLeft(wallet.blockchain.decimals())
                        .setScale(wallet.blockchain.decimals(), RoundingMode.DOWN)
                    Amount(value = value, blockchain = wallet.blockchain)
                }
            )
        }
    }

    private fun updateWallet(cosmosAccountInfo: CosmosAccountInfo) {
        // Transactions are confirmed instantaneously
        wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        wallet.setAmount(cosmosAccountInfo.amount)
        accountNumber = cosmosAccountInfo.accountNumber
        sequenceNumber = cosmosAccountInfo.sequenceNumber
    }

    private fun buildTransaction(input: Cosmos.SigningInput, signer: TransactionSigner?): Result<String> {
        val outputResult: Result<Cosmos.SigningOutput> = AnySignerWrapper().sign(
            walletPublicKey = wallet.publicKey,
            publicKeyType = PublicKeyType.SECP256K1,
            input = input,
            coin = CoinType.COSMOS,
            parser = Cosmos.SigningOutput.parser(),
            signer = signer,
            curve = wallet.blockchain.getSupportedCurves().first(),
        )
        return when (outputResult) {
            is Result.Failure -> outputResult
            is Result.Success -> Result.Success(txBuilder.buildForSend(outputResult.data))
        }
    }
}