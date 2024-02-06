package com.tangem.blockchain.blockchains.xrp.network.rippled

import com.tangem.blockchain.blockchains.xrp.network.XrpFeeResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.AddHeaderInterceptor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RippledNetworkProvider(
    baseUrl: String,
    apiKeyHeader: Pair<String, String>? = null,
) : XrpNetworkProvider {

    override val baseUrl: String = baseUrl

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

                val stateDeferred = retryIO { async { api.getServerState() } }

                val accountData = accountDeferred.await()
                val unconfirmedData = unconfirmedDeferred.await()
                val serverState = stateDeferred.await()

                val reserveBase = serverState.result!!.state!!.validatedLedger!!.reserveBase!!
                    .toBigDecimal().movePointLeft(decimals)

                if (accountData.result!!.errorCode == ERROR_CODE) {
                    Result.Success(
                        XrpInfoResponse(
                            reserveBase = reserveBase,
                            accountFound = false,
                        ),
                    )
                } else {
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
                            reserveBase = reserveBase,
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

    private companion object {
        private const val ERROR_CODE = 19
    }
}

private fun makeAccountBody(address: String, validated: Boolean): RippledBody {
    val params = HashMap<String, String>()
    params["account"] = address
    params["ledger_index"] = if (validated) "validated" else "current"
    return RippledBody(RippledMethod.ACCOUNT_INFO.value, listOf(params))
}

private fun makeSubmitBody(transaction: String): RippledBody {
    val params = HashMap<String, String>()
    params["tx_blob"] = transaction
    return RippledBody(RippledMethod.SUBMIT.value, listOf(params))
}
