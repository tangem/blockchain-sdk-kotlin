package com.tangem.blockchain.blockchains.ton.network

import com.squareup.moshi.Types
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.moshi

class TonJsonRpcNetworkProvider(
    override val baseUrl: String,
    private val api: TonApi,
) : TonNetworkProvider {

    private val walletInfoAdapter = moshi.adapter<TonProviderResponse<TonGetWalletInfoResponse>>(
        Types.newParameterizedType(
            TonProviderResponse::class.java,
            TonGetWalletInfoResponse::class.java,
        ),
    )
    private val feeAdapter = moshi.adapter<TonProviderResponse<TonGetFeeResponse>>(
        Types.newParameterizedType(
            TonProviderResponse::class.java,
            TonGetFeeResponse::class.java,
        ),
    )
    private val sendAdapter = moshi.adapter<TonProviderResponse<TonSendBocResponse>>(
        Types.newParameterizedType(
            TonProviderResponse::class.java,
            TonSendBocResponse::class.java,
        ),
    )

    override suspend fun getWalletInformation(address: String): Result<TonGetWalletInfoResponse> {
        return try {
            val response = api.post(TonProviderMethod.GetInfo(address = address).asRequestBody())
            val getWalletInfoResponse = requireNotNull(
                value = walletInfoAdapter.fromJson(response.string()),
                lazyMessage = { "Can not parse response" },
            )
            if (getWalletInfoResponse.ok) {
                Result.Success(getWalletInfoResponse.result)
            } else {
                val error = BlockchainSdkError.Ton.Api(
                    code = getWalletInfoResponse.code ?: 0,
                    message = getWalletInfoResponse.error.orEmpty(),
                )
                Result.Failure(error)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(address: String, message: String): Result<TonGetFeeResponse> {
        return try {
            val response = api.post(TonProviderMethod.EstimateFee(address = address, body = message).asRequestBody())
            val getFeeResponse = requireNotNull(
                value = feeAdapter.fromJson(response.string()),
                lazyMessage = { "Can not parse response" },
            )
            if (getFeeResponse.ok) {
                Result.Success(getFeeResponse.result)
            } else {
                val error = BlockchainSdkError.Ton.Api(
                    code = getFeeResponse.code ?: 0,
                    message = getFeeResponse.error.orEmpty(),
                )
                Result.Failure(error)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun send(message: String): Result<TonSendBocResponse> {
        return try {
            val response = api.post(TonProviderMethod.SendBocReturnHash(message = message).asRequestBody())
            val sendBocResponse = requireNotNull(
                value = sendAdapter.fromJson(response.string()),
                lazyMessage = { "Can not parse response" },
            )
            if (sendBocResponse.ok) {
                Result.Success(sendBocResponse.result)
            } else {
                val error = BlockchainSdkError.Ton.Api(
                    code = sendBocResponse.code ?: 0,
                    message = sendBocResponse.error.orEmpty(),
                )
                Result.Failure(error)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }
}
