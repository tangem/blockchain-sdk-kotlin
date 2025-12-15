package com.tangem.blockchain.blockchains.xrp.network.rippled

import com.tangem.blockchain.blockchains.xrp.network.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.createRetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RippledNetworkProvider(
    override val baseUrl: String,
    apiKeyHeader: Pair<String, String>? = null,
) : XrpNetworkProvider {

    private val api: RippledApi by lazy {
        createRetrofitInstance(
            baseUrl = baseUrl,
            headerInterceptors = listOf(
                AddHeaderInterceptor(
                    headers = buildMap {
                        put("Content-Type", "application/json")
                        put("User-Agent", "Never-mind")
                        apiKeyHeader?.let { put(key = it.first, value = it.second) }
                    },
                ),
            ),
        ).create(RippledApi::class.java)
    }
    private val decimals = Blockchain.XRP.decimals()

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> {
        return try {
            coroutineScope {
                val accountBody = makeAccountBody(address, validated = true)
                val accountDeferred = retryIO { async { api.getAccount(accountBody) } }

                val unconfirmedBody = makeAccountBody(address, validated = false)
                val unconfirmedDeferred = retryIO { async { api.getAccount(unconfirmedBody) } }

                val accountLineBody = makeAccountLinesBody(address)
                val accountLineDeferred = retryIO { async { api.getAccountLine(accountLineBody) } }

                val stateDeferred = retryIO { async { api.getServerState() } }

                val accountData = accountDeferred.await()
                val unconfirmedData = unconfirmedDeferred.await()
                val accountLineData = accountLineDeferred.await()
                val serverState = stateDeferred.await()

                val validatedLedger = serverState.result!!.state!!.validatedLedger!!
                val reserveBase = validatedLedger.reserveBase!!
                    .toBigDecimal().movePointLeft(decimals)

                val ownerCount = accountData.result?.accountData?.ownerCount ?: 0
                val reserveTotal = (validatedLedger.reserveBase!! + validatedLedger.reserveInc!! * ownerCount)
                    .toBigDecimal()
                    .movePointLeft(decimals)

                if (accountData.result!!.errorCode == ERROR_CODE) {
                    Result.Success(
                        XrpInfoResponse(
                            reserveBase = reserveBase,
                            reserveTotal = reserveTotal,
                            reserveInc = validatedLedger.reserveInc.toBigDecimal().movePointLeft(decimals),
                            accountFound = false,
                            tokenBalances = setOf(),
                        ),
                    )
                } else {
                    val tokenBalances = accountLineData.result?.lines?.mapTo(mutableSetOf()) {
                        XrpTokenBalance(
                            balance = it.balance.toBigDecimalOrDefault(),
                            issuer = it.account,
                            currency = it.currency,
                            noRipple = it.noRipple == true,
                        )
                    } ?: setOf()
                    val confirmedBalance =
                        accountData.result!!.accountData!!.balance!!.toBigDecimal()
                            .movePointLeft(decimals)
                    val unconfirmedBalance =
                        unconfirmedData.result!!.accountData!!.balance!!.toBigDecimal()
                            .movePointLeft(decimals)

                    Result.Success(
                        XrpInfoResponse(
                            balance = confirmedBalance,
                            sequence = accountData.result!!.accountData!!.sequence!!,
                            hasUnconfirmed = confirmedBalance != unconfirmedBalance,
                            reserveTotal = reserveTotal,
                            reserveBase = reserveBase,
                            reserveInc = validatedLedger.reserveInc.toBigDecimal().movePointLeft(decimals),
                            tokenBalances = tokenBalances,
                        ),
                    )
                }
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(): Result<XrpFeeResponse> {
        return try {
            val feeData = retryIO { api.getFee() }
            Result.Success(
                XrpFeeResponse(
                    feeData.result!!.feeData!!.minimalFee!!.toBigDecimal().movePointLeft(decimals),
                    feeData.result!!.feeData!!.normalFee!!.toBigDecimal().movePointLeft(decimals),
                    feeData.result!!.feeData!!.priorityFee!!.toBigDecimal().movePointLeft(decimals),
                ),
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val submitBody = makeSubmitBody(transaction)
            val submitData = retryIO { api.submitTransaction(submitBody) }
            val result = submitData.result!!
            if (result.resultCode == 0) {
                SimpleResult.Success
            } else {
                if (result.resultMessage == "Held until escalated fee drops.") {
                    SimpleResult.Success
                } else {
                    SimpleResult.Failure(
                        BlockchainSdkError.CustomError(
                            result.resultMessage ?: result.errorException ?: "Unknown error message",
                        ),
                    )
                }
            }
        } catch (exception: Exception) {
            SimpleResult.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun checkIsAccountCreated(address: String): Boolean { // TODO: return result?
        return try {
            val accountBody = makeAccountBody(address, validated = true)
            val accountData = retryIO { api.getAccount(accountBody) }
            accountData.result!!.errorCode != ERROR_CODE
        } catch (exception: Exception) {
            true // or let's assume it's created? (normally it is)
        }
    }

    override suspend fun checkTargetAccount(address: String, token: Token?): Result<XrpTargetAccountResponse> {
        return try {
            coroutineScope {
                val checkIsAccountCreatedDeferred = retryIO { async { checkIsAccountCreated(address) } }
                val isAccountCreated = checkIsAccountCreatedDeferred.await()
                if (token == null) {
                    val response = XrpTargetAccountResponse(
                        accountCreated = isAccountCreated,
                        trustlineCreated = null,
                    )
                    return@coroutineScope Result.Success(response)
                }

                val accountLineBody = makeAccountLinesBody(address)
                val accountLineDeferred = retryIO { async { api.getAccountLine(accountLineBody) } }
                val accountLineData = accountLineDeferred.await()
                val isExist = accountLineData.result?.lines
                    ?.any { "${it.currency}.${it.account}" == token.contractAddress }
                val response = XrpTargetAccountResponse(
                    accountCreated = isAccountCreated,
                    trustlineCreated = isExist,
                )
                Result.Success(response)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun hasTransferRate(address: String): Boolean {
        return try {
            val accountBody = makeAccountBody(address, validated = true)
            val accountData = retryIO { api.getAccount(accountBody) }
            val transferRate = accountData.result?.accountData?.transferRate
            transferRate != null && transferRate != ZERO_TRANSFER_RATE && transferRate != ZERO
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getSequence(address: String): Result<Long> {
        return try {
            val accountBody = makeAccountBody(address, validated = true)
            val sequence = api.getAccount(accountBody).result?.accountData?.sequence ?: 0
            Result.Success(sequence)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun checkDestinationTagRequired(address: String): Boolean {
        return try {
            val accountBody = makeAccountBody(address, validated = true)
            val accountData = retryIO { api.getAccount(accountBody) }
            accountData.result?.accountFlags?.requireDestinationTag == true
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val ERROR_CODE = 19
        const val ZERO = 0L
        const val ZERO_TRANSFER_RATE = 1_000_000_000L // 1 billion means no transfer rate
    }
}

private fun makeAccountBody(address: String, validated: Boolean): RippledBody {
    val params = HashMap<String, String>()
    params["account"] = address
    params["ledger_index"] = if (validated) "validated" else "current"
    return RippledBody(RippledMethod.ACCOUNT_INFO.value, listOf(params))
}

private fun makeAccountLinesBody(address: String): RippledBody {
    val params = HashMap<String, Any>()
    params["account"] = address
    params["api_version"] = 2
    return RippledBody(RippledMethod.ACCOUNT_LINES.value, listOf(params))
}

private fun makeSubmitBody(transaction: String): RippledBody {
    val params = HashMap<String, String>()
    params["tx_blob"] = transaction
    return RippledBody(RippledMethod.SUBMIT.value, listOf(params))
}