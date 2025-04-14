package com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.createRetrofitInstance

internal class PolkadotAccountHealthCheckNetworkService(override val baseUrl: String) :
    PolkadotAccountHealthCheckNetworkProvider {

    private val api = createRetrofitInstance(baseUrl).create(PolkadotAccountHealthCheckApi::class.java)
    override suspend fun getExtrinsicsList(address: String, afterExtrinsicId: Long?): Result<ExtrinsicListResponse> {
        return retryIO {
            api.getExtrinsicList(
                ExtrinsicsListBody(
                    address = address,
                    afterExtrinsicId = afterExtrinsicId,
                    order = EXTRINSICS_ORDER,
                    row = EXTRINSICS_ROW_COUNT,
                    page = EXTRINSICS_PAGE,
                ),
            ).toResponse()
        }
    }

    override suspend fun getExtrinsicDetail(hash: String): Result<ExtrinsicDetailResponse> {
        return retryIO {
            api.getExtrinsicDetail(
                ExtrinsicDetailBody(hash = hash),
            ).toResponse()
        }
    }

    override suspend fun getAccountInfo(key: String): Result<AccountResponse> {
        return retryIO {
            api.getAccountInfo(
                AccountBody(key = key),
            ).toResponse()
        }
    }

    private fun <T> PolkadotAccountHealthCheckResponse<T>.toResponse(): Result<T> {
        return if (message == SUCCESS_MESSAGE && data != null) {
            Result.Success(data)
        } else {
            Result.Failure(BlockchainSdkError.CustomError(message ?: "Unknown error"))
        }
    }

    private companion object {
        const val SUCCESS_MESSAGE = "Success"
        const val EXTRINSICS_ORDER = "asc" // From older to newer
        const val EXTRINSICS_PAGE = 0 // Other entries will request via `after_id`
        const val EXTRINSICS_ROW_COUNT = 100 // Maximum of entries to request
    }
}