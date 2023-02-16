package com.tangem.blockchain.blockchains.tron

import android.util.Log
import com.tangem.blockchain.blockchains.tron.network.TronAccountInfo
import com.tangem.blockchain.blockchains.tron.network.TronJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.calculateSha256
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import kotlin.math.ceil

class TronWalletManager(
    wallet: Wallet,
    private val transactionBuilder: TronTransactionBuilder,
    networkProvider: TronJsonRpcNetworkProvider,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String = networkProvider.network.url

    private val networkService = TronNetworkService(networkProvider, wallet.blockchain)
    private val dummySigner = DummySigner()

    override suspend fun update() {
        val transactionIds = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .mapNotNull { it.hash }

        when (val response =
            networkService.getAccountInfo(wallet.address, cardTokens, transactionIds)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: TronAccountInfo) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.amounts[AmountType.Coin]?.value = response.balance
        response.tokenBalances.forEach { wallet.addTokenValue(it.value, it.key) }

        wallet.recentTransactions.forEach() {
            if (response.confirmedTransactionIds.contains(it.hash)) {
                it.status = TransactionStatus.Confirmed
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner
    ): SimpleResult {
        val signResult = signTransactionData(
            amount = transactionData.amount,
            source = wallet.address,
            destination = transactionData.destinationAddress,
            signer = signer,
            publicKey = wallet.publicKey
        )
        when (signResult) {
            is Result.Failure -> return SimpleResult.Failure(signResult.error)
            is Result.Success -> {
                return when (val sendResult = networkService.broadcastHex(signResult.data)) {
                    is Result.Failure -> SimpleResult.Failure(sendResult.error)
                    is Result.Success -> {
                        wallet.addOutgoingTransaction(
                            transactionData.copy(hash = sendResult.data.txid)
                        )
                        SimpleResult.Success
                    }
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val contractAddress = if (amount.type is AmountType.Token) {
            amount.type.token.contractAddress
        } else {
            null
        }

        val blockchain = wallet.blockchain
        return coroutineScope {
            val chainParametersDef = async { networkService.getChainParameters() }
            val maxEnergyDef = async { networkService.getMaxEnergyUse(contractAddress) }
            val resourceDef = async { networkService.getAccountResource(wallet.address) }
            val destinationExistsDef = async { networkService.checkIfAccountExists(destination) }
            val transactionDataDef = async {
                signTransactionData(
                    amount, wallet.address, destination, dummySigner, dummySigner.publicKey
                )
            }

            if (!destinationExistsDef.await() && amount.type == AmountType.Coin) {
                return@coroutineScope Result.Success(
                    listOf(
                        Amount(
                            BigDecimal.valueOf(1.1),
                            blockchain
                        )
                    )
                )
            }

            val chainParameters = when (val chainParametersResult = chainParametersDef.await()) {
                is Result.Failure -> return@coroutineScope Result.Failure(chainParametersResult.error)
                is Result.Success -> chainParametersResult.data
            }
            val maxEnergyUse = when (val maxEnergyResult = maxEnergyDef.await()) {
                is Result.Failure -> return@coroutineScope Result.Failure(maxEnergyResult.error)
                is Result.Success -> maxEnergyResult.data
            }
            val resource = when (val resourceResult = resourceDef.await()) {
                is Result.Failure -> return@coroutineScope Result.Failure(resourceResult.error)
                is Result.Success -> resourceResult.data
            }
            val transactionData = when (val transactionDataResult = transactionDataDef.await()) {
                is Result.Failure -> return@coroutineScope Result.Failure(transactionDataResult.error)
                is Result.Success -> transactionDataResult.data
            }

            val sunPerBandwidthPoint = 1000

            val dynamicEnergyMaxFactor =
                chainParameters.dynamicEnergyMaxFactor.toDouble() / ENERGY_FACTOR_PRECISION

            val additionalDataSize = 64
            val transactionSizeFee =
                sunPerBandwidthPoint * (transactionData.size + additionalDataSize)
            val sunPerEnergyUnit = chainParameters.sunPerEnergyUnit
            val maxEnergyFee =
                ceil((maxEnergyUse * sunPerEnergyUnit).toDouble() * (1 + dynamicEnergyMaxFactor)).toLong()
            val totalFee = transactionSizeFee + maxEnergyFee
            val remainingBandwidthInSun =
                (resource.freeNetLimit - (resource.freeNetUsed ?: 0)) * sunPerBandwidthPoint

            if (totalFee <= remainingBandwidthInSun) {
                return@coroutineScope Result.Success(listOf(Amount(blockchain)))
            } else {
                val value = BigDecimal(totalFee).movePointLeft(blockchain.decimals())
                return@coroutineScope Result.Success(listOf(Amount(value, blockchain)))
            }
        }
    }

    private suspend fun signTransactionData(
        amount: Amount,
        source: String,
        destination: String,
        signer: TransactionSigner,
        publicKey: Wallet.PublicKey
    ): Result<ByteArray> {

        return when (val result = networkService.getNowBlock()) {
            is Result.Failure -> {
                Result.Failure(result.error)
            }
            is Result.Success -> {
                val transactionToSign = transactionBuilder.buildForSign(
                    amount, source, destination, result.data
                )
                when (val signResult =
                    sign(transactionToSign.encode().calculateSha256(), signer, publicKey)) {
                    is Result.Failure -> Result.Failure(signResult.error)
                    is Result.Success -> {
                        val transactionToSend = transactionBuilder.buildForSend(
                            rawData = transactionToSign, signature = signResult.data
                        )
                        Result.Success(transactionToSend.encode())
                    }
                }
            }
        }
    }

    private suspend fun sign(
        transactionToSign: ByteArray,
        signer: TransactionSigner,
        publicKey: Wallet.PublicKey
    ): Result<ByteArray> {
        return when (val result = signer.sign(transactionToSign, publicKey)) {
            is CompletionResult.Success -> {
                val unmarshalledSignature =  if (publicKey == dummySigner.publicKey) {
                    result.data + ByteArray(1)
                } else {
                    transactionBuilder.unmarshalSignature(result.data, transactionToSign, publicKey)
                }
                Result.Success(unmarshalledSignature)
            }
            is CompletionResult.Failure -> {
                Result.fromTangemSdkError(result.error)
            }
        }
    }
}

/**
 * Value taken from [TIP-491](https://github.com/tronprotocol/tips/issues/491)
 */
private const val ENERGY_FACTOR_PRECISION = 10_000
