package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_STELLAR
import com.tangem.blockchain.network.API_STELLAR_TESTNET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.stellar.sdk.AssetTypeCreditAlphaNum
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.FeeStatsResponse
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StellarNetworkService(isTestnet: Boolean) : StellarNetworkProvider {
    override val host: String = if (isTestnet) API_STELLAR_TESTNET else API_STELLAR
    private val blockchain = Blockchain.Stellar
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
    private val recordsLimitCap = 200
    private val decimals = blockchain.decimals()

    val network: Network = if (isTestnet) Network.TESTNET else Network.PUBLIC
    private val stellarServer by lazy { Server(host) }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = stellarServer.submitTransaction(Transaction.fromEnvelopeXdr(transaction, network))
            if (response.isSuccess) {
                SimpleResult.Success
            } else {
                val trResultCode = response.extras?.resultCodes?.transactionResultCode
                val operationResultCode = response.extras?.resultCodes?.operationsResultCodes?.getOrNull(0) ?: ""
                var trResult: String = trResultCode + operationResultCode

                if (trResult == "tx_too_late") trResult = "The system time is invalid"

                SimpleResult.Failure(BlockchainSdkError.CustomError(trResult))
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun checkTargetAccount(
        address: String,
        token: Token?,
    ): Result<StellarTargetAccountResponse> {
        return try {
            val account = stellarServer.accounts().account(address)

            if (token == null) { // xlm transaction
                Result.Success(StellarTargetAccountResponse(accountCreated = true))
            } else { // token transaction
                val tokenBalance = account.balances
                    .filter { it.assetCode == token.symbol }
                    .find { it.assetIssuer == token.contractAddress } // null if trustline not created
                Result.Success(
                    StellarTargetAccountResponse(
                        accountCreated = true,
                        trustlineCreated = tokenBalance != null
                    )
                )
            }
        } catch (errorResponse: ErrorResponse) {
            if (errorResponse.code == 404) {
                Result.Success(StellarTargetAccountResponse(accountCreated = false))
            } else {
                Result.Failure(errorResponse.toBlockchainSdkError())
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getInfo(accountId: String): Result<StellarResponse> {
        return try {
            coroutineScope {
                val accountResponseDeferred = async(Dispatchers.IO) { stellarServer.accounts().account(accountId) }
                val ledgerResponseDeferred = async(Dispatchers.IO) {
                    val latestLedger: Int = stellarServer.root().historyLatestLedger
                    stellarServer.ledgers().ledger(latestLedger.toLong())
                }
                val paymentsResponseDeferred = async(Dispatchers.IO) {
                    stellarServer.payments().forAccount(accountId).order(RequestBuilder.Order.DESC).execute()
                }

                val accountResponse = accountResponseDeferred.await()
                val coinBalance = accountResponse.balances
                    .find { it.asset is AssetTypeNative }?.balance?.toBigDecimal()
                    ?: return@coroutineScope Result.Failure(
                        BlockchainSdkError.CustomError("Stellar Balance not found")
                    )

                val tokenBalances = accountResponse.balances.filter { it.asset !is AssetTypeNative }.map {
                    StellarAssetBalance(it.balance.toBigDecimal(), it.assetCode, it.assetIssuer)
                }

                val ledgerResponse = ledgerResponseDeferred.await()
                val baseFee = ledgerResponse.baseFeeInStroops.toBigDecimal().movePointLeft(decimals)
                val baseReserve = ledgerResponse.baseReserveInStroops.toBigDecimal().movePointLeft(decimals)

                val recentTransactions = paymentsResponseDeferred.await().records.mapNotNull { it.toTransactionData() }

                Result.Success(
                    StellarResponse(
                        coinBalance = coinBalance,
                        tokenBalances = tokenBalances.toSet(),
                        baseFee = baseFee,
                        baseReserve = baseReserve,
                        sequence = accountResponse.sequenceNumber,
                        recentTransactions = recentTransactions,
                        subEntryCount = accountResponse.subentryCount
                    )
                )
            }
        } catch (exception: Exception) {
            if (exception is ErrorResponse && exception.code == 404) {
                Result.Failure(BlockchainSdkError.AccountNotFound)
            } else {
                Result.Failure(exception.toBlockchainSdkError())
            }
        }
    }

    override suspend fun getFeeStats(): Result<FeeStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val feeStats = stellarServer.feeStats().execute()
            Result.Success(feeStats)
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(accountId: String): Result<Int> {
        return try {
            coroutineScope {
                var operationsPage = stellarServer.operations().forAccount(accountId)
                    .limit(recordsLimitCap)
                    .includeFailed(true)
                    .execute()
                val operations = operationsPage.records

                while (operationsPage.records.size == recordsLimitCap) {
                    try {
                        operationsPage = operationsPage.getNextPage(stellarServer.httpClient)
                        operations.addAll(operationsPage.records)
                    } catch (e: URISyntaxException) {
                        break
                    }
                }
                Result.Success(operations.filter { it.sourceAccount == accountId }.size)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private fun OperationResponse.toTransactionData(): TransactionData? {
        return when (this) {
            is PaymentOperationResponse -> this.toTransactionData()
            is CreateAccountOperationResponse -> this.toTransactionData()
            else -> null
        }
    }

    private fun PaymentOperationResponse.toTransactionData(): TransactionData {
        val amount = when (val asset = asset) {
            is AssetTypeNative -> Amount(amount.toBigDecimal(), blockchain)
            is AssetTypeCreditAlphaNum -> Amount(
                currencySymbol = asset.code,
                value = amount.toBigDecimal(),
                decimals = decimals,
                type = AmountType.Token(Token(asset.code, asset.issuer, decimals))
            )
            else -> throw Exception("Unknown asset type")
        }
        return TransactionData(
            amount = amount,
            fee = null,
            sourceAddress = from,
            destinationAddress = to,
            status = TransactionStatus.Confirmed,
            date = Calendar.getInstance().apply { time = dateFormat.parse(createdAt)!! },
            hash = transactionHash
        )
    }

    private fun CreateAccountOperationResponse.toTransactionData(): TransactionData {
        return TransactionData(
            amount = Amount(startingBalance.toBigDecimal(), blockchain),
            fee = null,
            sourceAddress = funder,
            destinationAddress = account,
            status = TransactionStatus.Confirmed,
            date = Calendar.getInstance().apply { time = dateFormat.parse(createdAt)!! },
            hash = transactionHash
        )
    }
}