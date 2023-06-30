package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import org.stellar.sdk.*
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.*
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val RECORD_LIMIT = 200

class StellarNetworkService(hosts: List<StellarNetwork>, isTestnet: Boolean) : StellarNetworkProvider {

    private val blockchain = Blockchain.Stellar
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
    private val decimals = blockchain.decimals()

    val network: Network = if (isTestnet) Network.TESTNET else Network.PUBLIC
    private val stellarMultiProvider = MultiNetworkProvider(
        hosts.map {
            Server(it.url)
        }
    )

    // using reflect api to get serverUri for currentProvider cause lib doesn't provide this
    override val host: String =
        (stellarMultiProvider.currentProvider.getPrivateProperty("serverURI") as? HttpUrl)?.host ?: ""

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = stellarMultiProvider.performRequest(
                Server::submitTransaction, Transaction.fromEnvelopeXdr(transaction, network) as Transaction
            ).successOr {
                return SimpleResult.Failure(it.error)
            }
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

            val account = stellarMultiProvider.performRequest(Server::accountCall, address).successOr {
                return Result.Failure(it.error)
            }

            if (token == null) { // xlm transaction
                Result.Success(StellarTargetAccountResponse(accountCreated = true))
            } else { // token transaction
                // tokenBalance can be null if trustline not created
                val tokenBalance = account.balances
                    .filter { it.assetCode.isPresent() && it.assetIssuer.isPresent() }
                    .filter { it.assetCode.get() == token.symbol }
                    .find { it.assetIssuer.get() == token.contractAddress } // null if trustline not created
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
                val accountResponseDeferred = async(Dispatchers.IO) {
                    stellarMultiProvider.performRequest(Server::accountCall, accountId).successOr {
                        throw it.error
                    }
                }
                val ledgerResponseDeferred = async(Dispatchers.IO) {
                    val latestLedger = stellarMultiProvider.performRequest(Server::rootCall).successOr {
                        throw it.error
                    }.historyLatestLedger

                    stellarMultiProvider.performRequest(Server::ledgerCall, latestLedger.toLong()).successOr {
                        throw it.error
                    }
                }
                val paymentsResponseDeferred = async(Dispatchers.IO) {
                    stellarMultiProvider.performRequest(Server::paymentsCall, accountId).successOr {
                        throw it.error
                    }
                }

                val accountResponse = accountResponseDeferred.await()
                val coinBalance = accountResponse.balances
                    .find { it.asset.isPresent() && it.asset.get() is AssetTypeNative }
                    ?.balance?.toBigDecimal()
                    ?: return@coroutineScope Result.Failure(
                        BlockchainSdkError.CustomError("Stellar Balance not found")
                    )

                val tokenBalances = accountResponse.balances
                    .filter { it.asset.isPresent() && it.assetCode.isPresent() && it.assetIssuer.isPresent() }
                    .filter { it.asset.get() !is AssetTypeNative }
                    .map {
                        StellarAssetBalance(it.balance.toBigDecimal(), it.assetCode.get(), it.assetIssuer.get())
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
        stellarMultiProvider.performRequest(Server::feeCall)
    }

    override suspend fun getSignatureCount(accountId: String): Result<Int> {
        return try {
            coroutineScope {
                var operationsPage = stellarMultiProvider.performRequest(Server::operationsLimit, accountId).successOr {
                    throw it.error
                }
                val operations = operationsPage.records

                while (operationsPage.records.size == RECORD_LIMIT) {
                    try {
                        operationsPage = operationsPage.getNextPage(stellarMultiProvider.currentProvider.httpClient)
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

fun <T : Any> T.getPrivateProperty(variableName: String): Any? {
    return javaClass.getDeclaredField(variableName).let { field ->
        field.isAccessible = true
        return@let field.get(this)
    }
}

/** Necessary extensions for Server, without it not able to use performRequest
 * of MultiNetworkProvider to call lambda
 */
fun Server.accountCall(data: String): AccountResponse {
    return this.accounts().account(data)
}

fun Server.rootCall(): RootResponse {
    return this.root()
}

fun Server.ledgerCall(ledgerSeq: Long): LedgerResponse {
    return this.ledgers().ledger(ledgerSeq)
}

fun Server.paymentsCall(accountId: String): Page<OperationResponse> {
    return this.payments().forAccount(accountId).order(RequestBuilder.Order.DESC).execute()
}

fun Server.feeCall(): FeeStatsResponse {
    return this.feeStats().execute()
}

fun Server.operationsLimit(accountId: String): Page<OperationResponse> {
    return this.operations().forAccount(accountId)
        .limit(RECORD_LIMIT)
        .includeFailed(true)
        .execute()
}