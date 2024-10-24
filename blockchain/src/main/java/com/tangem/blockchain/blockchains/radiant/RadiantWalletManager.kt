package com.tangem.blockchain.blockchains.radiant

import android.util.Log
import com.tangem.blockchain.blockchains.radiant.models.RadiantAccountInfo
import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.createWalletCorePublicKey
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.common.CompletionResult
import wallet.core.jni.PublicKeyType
import java.math.BigDecimal
import java.math.RoundingMode

internal class RadiantWalletManager(
    wallet: Wallet,
    networkProviders: List<ElectrumNetworkProvider>,
) : WalletManager(wallet) {

    private val networkService = RadiantNetworkService(networkProviders)
    private val blockchain = wallet.blockchain
    private val addressScriptHash by lazy { RadiantAddressUtils.generateAddressScriptHash(wallet.address) }
    private val transactionBuilder = RadiantTransactionBuilder(
        publicKey = wallet.publicKey,
        decimals = blockchain.decimals(),
    )

    override val currentHost: String get() = networkService.baseUrl

    override suspend fun updateInternal() {
        when (val result = networkService.getInfo(scriptHash = addressScriptHash)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(accountModel: RadiantAccountInfo) {
        if (accountModel.balance != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.setCoinValue(accountModel.balance)
        transactionBuilder.setUnspentOutputs(accountModel.unspentOutputs)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage, error)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return try {
            val hashesForSign = transactionBuilder.buildForSign(transactionData)
            when (val signatureResult = signer.sign(hashes = hashesForSign, publicKey = wallet.publicKey)) {
                is CompletionResult.Success -> {
                    val signatures = signatureResult.data
                    val walletCorePublicKey =
                        createWalletCorePublicKey(wallet.publicKey.blockchainKey, PublicKeyType.SECP256K1)
                    if (signatures.count() != hashesForSign.count()) {
                        throw BlockchainSdkError.FailedToBuildTx
                    }

                    val isVerified = signatures.mapIndexed { index, sig ->
                        walletCorePublicKey.verifyAsDER(sig, hashesForSign[index].reversedArray())
                    }.none { it }
                    if (!isVerified) {
                        throw BlockchainSdkError.FailedToBuildTx
                    }
                    val rawTx = transactionBuilder.buildForSend(transactionData, signatures)
                    when (val sendResult = networkService.sendTransaction(rawTx)) {
                        is Result.Success -> {
                            val hash = sendResult.data
                            transactionData.hash = hash
                            wallet.addOutgoingTransaction(transactionData, hashToLowercase = false)

                            Result.Success(TransactionSendResult(hash))
                        }
                        is Result.Failure -> sendResult
                    }
                }
                is CompletionResult.Failure -> Result.fromTangemSdkError(signatureResult.error)
            }
        } catch (e: BlockchainSdkError) {
            Result.Failure(e)
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.FailedToSendException)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return try {
            when (val feeResult = networkService.getEstimatedFee(REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS)) {
                is Result.Failure -> return feeResult
                is Result.Success -> {
                    val transactionSize = transactionBuilder.estimateTransactionSize(
                        transactionData = TransactionData.Uncompiled(
                            amount = amount,
                            fee = Fee.Common(Amount(amount, feeResult.data.minimalPerKb)),
                            sourceAddress = wallet.address,
                            destinationAddress = destination,
                        ),
                    ).toBigDecimal()
                    val minFee = feeResult.data.minimalPerKb.calculateFee(transactionSize)
                    val normalFee = feeResult.data.normalPerKb.calculateFee(transactionSize)
                    val priorityFee = feeResult.data.priorityPerKb.calculateFee(transactionSize)
                    val fees = TransactionFee.Choosable(
                        minimum = Fee.Common(Amount(minFee, blockchain)),
                        normal = Fee.Common(Amount(normalFee, blockchain)),
                        priority = Fee.Common(Amount(priorityFee, blockchain)),
                    )
                    Result.Success(fees)
                }
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun BigDecimal.calculateFee(txSize: BigDecimal): BigDecimal = this
        .multiply(txSize)
        .divide(BigDecimal(BYTES_IN_KB))
        .setScale(wallet.blockchain.decimals(), RoundingMode.UP)

    private companion object {
        const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS = 10

        /**
         * We use 1000, because Electrum node return fee for per 1000 bytes.
         */
        const val BYTES_IN_KB = 1000
    }
}
