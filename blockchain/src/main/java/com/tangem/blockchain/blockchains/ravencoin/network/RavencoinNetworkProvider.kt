package com.tangem.blockchain.blockchains.ravencoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.extensions.hexToBytes
import retrofit2.create
import java.math.BigDecimal
import java.util.Calendar

private const val FEE_NUMBER_OF_BLOCKS = 10

class RavencoinNetworkProvider(
    override val baseUrl: String,
) : BitcoinNetworkProvider {

    private val api: RavencoinApi = createRetrofitInstance(baseUrl).create()

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val walletInfo = api.getWalletInfo(address)
            val utxo = api.getUTXO(address)

            val addressInfo = if (walletInfo.unconfirmedTxApperances != 0L) {
                val transactions = api.getTransactions(address).transactions
                mapToBitcoinResponse(walletInfo = walletInfo, outputs = utxo, transactions = transactions)
            } else {
                mapToBitcoinResponse(walletInfo = walletInfo, outputs = utxo, transactions = emptyList())
            }
            Result.Success(addressInfo)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        val feeResponse = api.getFee(numberOfBlocks = FEE_NUMBER_OF_BLOCKS)
        val feePerKb = feeResponse["$FEE_NUMBER_OF_BLOCKS"] ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        val minimalRate = feePerKb * BigDecimal.valueOf(1.1)
        val normalRate = feePerKb * BigDecimal.valueOf(1.3)
        val priorityRate = feePerKb * BigDecimal.valueOf(1.5)
        return Result.Success(
            BitcoinFee(
                minimalPerKb = minimalRate,
                normalPerKb = normalRate,
                priorityPerKb = priorityRate,
            )
        )
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            api.send(RavencoinRawTransactionRequest(transaction))
            SimpleResult.Success
        } catch (e: Exception) {
            SimpleResult.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        throw BlockchainSdkError.CustomError("Not yet implemented")
    }

    private fun mapToBitcoinResponse(
        walletInfo: RavencoinWalletInfoResponse,
        outputs: List<RavencoinWalletUTXOResponse>,
        transactions: List<RavencoinTransactionInfo>,
    ): BitcoinAddressInfo {
        return BitcoinAddressInfo(
            balance = walletInfo.balance ?: BigDecimal.ZERO,
            unspentOutputs = outputs.map { utxo ->
                BitcoinUnspentOutput(
                    amount = utxo.amount,
                    outputIndex = utxo.vout,
                    transactionHash = utxo.txid.hexToBytes(),
                    outputScript = utxo.scriptPubKey.hexToBytes(),
                )
            },
            recentTransactions = transactions
                .filter { it.confirmations == 0L || it.blockHeight == -1L }
                .mapNotNull { txInfo -> mapToBasicTransactionData(txInfo, walletInfo.address) },
            hasUnconfirmed = walletInfo.unconfirmedTxApperances != 0L,
        )
    }

    private fun mapToBasicTransactionData(
        transaction: RavencoinTransactionInfo,
        walletAddress: String,
    ): BasicTransactionData? {
        val isIncoming = transaction.vin.all { it.address != walletAddress }
        val hash = transaction.txid
        val timestamp = transaction.time * 1000
        val balanceDiff = if (isIncoming) {
            // Find all outputs to the our address
            val outputs = transaction.vout.filter {
                it.scriptPubKey.addresses.any { address -> address == walletAddress }
            }
            outputs
                .map { BigDecimal(it.value) }
                .reduce { acc, bigDecimal -> acc + bigDecimal }
        } else {
            // Find all outputs from the our address
            val outputs = transaction.vout.filter {
                it.scriptPubKey.addresses.any { address -> address != walletAddress }
            }
            outputs
                .map { BigDecimal(it.value) }
                .reduce { acc, bigDecimal -> acc + bigDecimal }
                .negate()
        }

        val otherAddress = transaction.vout
            .firstOrNull { vout -> vout.scriptPubKey.addresses.any { it != walletAddress } }
            ?.scriptPubKey
            ?.addresses
            ?.firstOrNull() ?: return null

        return BasicTransactionData(
            balanceDif = balanceDiff,
            hash = hash,
            date = Calendar.getInstance().apply { this.timeInMillis = timestamp },
            isConfirmed = transaction.confirmations != 0L,
            destination = if (isIncoming) walletAddress else otherAddress,
            source = if (isIncoming) otherAddress else walletAddress
        )
    }
}
