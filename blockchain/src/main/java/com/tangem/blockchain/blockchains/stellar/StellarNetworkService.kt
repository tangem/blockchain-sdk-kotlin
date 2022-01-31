package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_STELLAR
import com.tangem.blockchain.network.API_STELLAR_TESTNET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.stellar.sdk.*
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

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
            val response = stellarServer
                    .submitTransaction(Transaction.fromEnvelopeXdr(transaction, network))
            if (response.isSuccess) {
                SimpleResult.Success
            } else {
                var trResult: String = response.extras?.resultCodes?.transactionResultCode +
                        (response.extras?.resultCodes?.operationsResultCodes?.getOrNull(0)
                                ?: "")
                if (trResult == "tx_too_late") trResult = "The system time is invalid"
                SimpleResult.Failure(Exception(trResult))
            }
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    override suspend fun checkTargetAccount(
            address: String,
            token: Token?
    ): Result<StellarTargetAccountResponse> {
        return try {
            val account = stellarServer.accounts().account(address)

            if (token == null) { // xlm transaction
                Result.Success(StellarTargetAccountResponse(accountCreated = true))
            } else { // token transaction
                val tokenBalance = account.balances.filter { it.assetCode == token.symbol }
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
                Result.Failure(errorResponse)
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getInfo(accountId: String): Result<StellarResponse> {
        return try {
            coroutineScope {
                val accountResponseDeferred =
                        async(Dispatchers.IO) { stellarServer.accounts().account(accountId) }
                val ledgerResponseDeferred = async(Dispatchers.IO) {
                    val latestLedger: Int = stellarServer.root().historyLatestLedger
                    stellarServer.ledgers().ledger(latestLedger.toLong())
                }
                val paymentsResponseDeferred = async(Dispatchers.IO) {
                    stellarServer.payments().forAccount(accountId)
                            .order(RequestBuilder.Order.DESC)
                            .execute()
                }

                val accountResponse = accountResponseDeferred.await()
                val coinBalance = accountResponse.balances
                        .find { it.asset is AssetTypeNative }
                        ?.balance?.toBigDecimal()
                        ?: return@coroutineScope Result.Failure(Exception("Stellar Balance not found"))

                val tokenBalances = accountResponse.balances.filter { it.asset !is AssetTypeNative }.map {
                    StellarAssetBalance(it.balance.toBigDecimal(), it.assetCode, it.assetIssuer)
                }

                val ledgerResponse = ledgerResponseDeferred.await()
                val baseFee =
                        ledgerResponse.baseFeeInStroops.toBigDecimal().movePointLeft(decimals)
                val baseReserve =
                        ledgerResponse.baseReserveInStroops.toBigDecimal().movePointLeft(decimals)

                val recentTransactions =
                        paymentsResponseDeferred.await().records.mapNotNull { it.toTransactionData() }

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
        } catch (error: Exception) {
            if (error is ErrorResponse && error.code == 404) {
                Result.Failure(BlockchainSdkError.AccountNotFound)
            } else {
                Result.Failure(error)
            }
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
        } catch (error: Exception) {
            Result.Failure(error)
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
        val asset = asset
        val amount = when (asset) {
            is AssetTypeNative -> Amount(amount.toBigDecimal(), blockchain)
            is AssetTypeCreditAlphaNum -> Amount(
                    currencySymbol = asset.code,
                    value = amount.toBigDecimal(),
                    decimals = decimals,
                    type = AmountType.Token(Token(asset.code, asset.issuer, decimals))
            )
            else -> throw Exception("Unknown asset type")
        }
        val issuer = (asset as? AssetTypeCreditAlphaNum)?.issuer

        return TransactionData(
                amount = amount,
                fee = null,
                sourceAddress = from,
                destinationAddress = to,
                contractAddress = issuer,
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