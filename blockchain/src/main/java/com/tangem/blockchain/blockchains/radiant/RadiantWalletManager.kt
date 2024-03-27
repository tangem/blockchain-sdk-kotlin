package com.tangem.blockchain.blockchains.radiant

import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import java.math.BigDecimal
import java.math.RoundingMode

internal class RadiantWalletManager(
    wallet: Wallet,
    networkProviders: List<ElectrumNetworkProvider>,
) : WalletManager(wallet) {

    private val networkService = RadiantNetworkService(networkProviders)
    private val blockchain = wallet.blockchain

    override val currentHost: String get() = networkService.baseUrl

    override suspend fun updateInternal() {
        TODO("Not yet implemented")
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return try {
            when (val feeResult = networkService.getEstimatedFee(REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS)) {
                is Result.Failure -> return feeResult
                is Result.Success -> {
                    val transactionSize = 1661.toBigDecimal() // FIXME: Change with real value
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
        .setScale(wallet.blockchain.decimals(), RoundingMode.UP)

    private companion object {
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS = 10
    }
}