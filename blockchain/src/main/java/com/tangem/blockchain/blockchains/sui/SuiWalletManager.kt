package com.tangem.blockchain.blockchains.sui

import android.util.Log
import com.tangem.blockchain.blockchains.sui.model.SuiWalletInfo
import com.tangem.blockchain.blockchains.sui.network.SuiNetworkService
import com.tangem.blockchain.blockchains.sui.network.rpc.SuiJsonRpcProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class SuiWalletManager(
    wallet: Wallet,
    networkProviders: List<SuiJsonRpcProvider>,
    private val addressService: SuiAddressService,
) : WalletManager(wallet), UtxoBlockchainManager {

    private val networkService: SuiNetworkService = SuiNetworkService(
        providers = networkProviders,
    )
    private val txBuilder = SuiTransactionBuilder(
        walletAddress = wallet.address,
        publicKey = wallet.publicKey,
    )

    override val currentHost: String get() = networkService.host

    override val allowConsolidation: Boolean = true

    private var walletInfo: SuiWalletInfo? = null

    override suspend fun updateInternal() {
        when (val result = networkService.getInfo(wallet.address)) {
            is Result.Failure -> updateWithError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    override fun addToken(token: Token) {
        // We may receive trimmed contract address. In that case we need to fill it with heading zeros
        val fixed = addressService.reformatContractAddress(token.contractAddress) ?: return
        super.addToken(
            token.copy(
                contractAddress = fixed,
            ),
        )
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val info = walletInfo ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        val transactionOutput = txBuilder.buildForSend(info, transactionData, signer)
            .successOr { return it }

        val txResponse = networkService.executeTransaction(
            transactionHash = transactionOutput.unsignedTx,
            signature = transactionOutput.signature,
        ).successOr { return it }

        if (!txResponse.effects.status.isSuccess) {
            return Result.Failure(BlockchainSdkError.FailedToSendException)
        }

        val txHash = txResponse.digest

        wallet.addOutgoingTransaction(
            transactionData = transactionData,
            txHash = txHash,
            hashToLowercase = false,
        )

        return Result.Success(TransactionSendResult(txHash))
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val info = walletInfo ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        val gasPrice = networkService.getReferenceGasPrice()
            .successOr { return it }
        val transactionHash = txBuilder.buildForDryRun(info, amount, destination, gasPrice)
            .successOr { return it }
        val txResponse = networkService.dryRunTransaction(transactionHash)
            .successOr { return it }

        if (!txResponse.effects.status.isSuccess) {
            return txBuilder.checkOnFailureFeeLoad(info, amount)
        }

        val gasUsed = txResponse.effects.gasUsed
        val totalGasMist = gasUsed.computationCost + gasUsed.storageCost

        val feeAmount = Amount(
            blockchain = Blockchain.Sui,
            value = totalGasMist.movePointLeft(Blockchain.Sui.decimals()),
        )
        val fee = Fee.Sui(
            amount = feeAmount,
            gasPrice = txResponse.input.gasData.price.toLong(),
            gasBudget = totalGasMist.toLong(),
        )

        return Result.Success(TransactionFee.Single(fee))
    }

    private suspend fun updateWallet(info: SuiWalletInfo) {
        val infoWithFixedAddresses = info.copy(
            coins = info.coins.map { coin ->
                // We may receive trimmed contract address. In that case we need to fill it with heading zeros
                coin.copy(coinType = addressService.reformatContractAddress(coin.coinType) ?: coin.coinType)
            },
        )
        walletInfo = infoWithFixedAddresses

        wallet.setAmount(Amount(infoWithFixedAddresses.suiTotalBalance, wallet.blockchain))
        cardTokens.forEach { token ->
            val tokenBalance = infoWithFixedAddresses.coins
                .filter { it.coinType == token.contractAddress }
                .sumOf { it.mistBalance }
                .movePointLeft(token.decimals)
            wallet.addTokenValue(tokenBalance, token)
        }

        checkUncompletedTransactions()
    }

    private suspend fun checkUncompletedTransactions() {
        val uncompletedTransactions = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }

        coroutineScope {
            uncompletedTransactions.map { uncompletedTx ->
                async {
                    removeTxIfCompleted(uncompletedTx)
                }
            }.awaitAll()
        }
    }

    private suspend fun removeTxIfCompleted(uncompletedTx: TransactionData.Uncompiled) {
        val hash = uncompletedTx.hash ?: return

        if (networkService.isTransactionConfirmed(hash)) {
            wallet.recentTransactions.remove(uncompletedTx)
        }
    }

    private fun updateWithError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }
}