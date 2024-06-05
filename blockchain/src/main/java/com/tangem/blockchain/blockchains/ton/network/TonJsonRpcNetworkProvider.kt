package com.tangem.blockchain.blockchains.ton.network

import com.squareup.moshi.Types
import com.tangem.Log
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.hexToBytes
import io.ktor.util.*
import okhttp3.Interceptor
import org.ton.block.AddrStd
import org.ton.block.MsgAddressInt
import org.ton.boc.BagOfCells
import org.ton.cell.CellBuilder
import org.ton.tlb.storeTlb
import java.math.BigDecimal
import java.math.BigInteger

class TonJsonRpcNetworkProvider(
    override val baseUrl: String,
    headerInterceptors: List<Interceptor> = emptyList(),
) : TonNetworkProvider {

    private val api: TonApi by lazy {
        createRetrofitInstance(baseUrl, headerInterceptors)
            .create(TonApi::class.java)
    }

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

    private val runGetMethodAdapter = moshi.adapter<TonProviderResponse<TonRunGetMethodResponse>>(
        Types.newParameterizedType(
            TonProviderResponse::class.java,
            TonRunGetMethodResponse::class.java,
        ),
    )

    override suspend fun getWalletInformation(address: String): Result<TonGetWalletInfoResponse> {
        return try {
            val response = api.post(TonProviderMethod.GetWalletInformation(address = address).asRequestBody())
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
    override suspend fun getJettonWalletAddress(input: GetJettonWalletAddressInput): Result<String> {
        return try {
            val ownerAddressBase64 = BagOfCells(
                CellBuilder().storeTlb(
                    MsgAddressInt.tlbCodec(),
                    MsgAddressInt(input.ownerAddress)
                ).build()
            ).toByteArray().encodeBase64()
            val response = runGetMethod(
                contractAddress = input.jettonMinterAddress,
                method = GET_WALLET_ADDRESS_METHOD,
                stack = listOf(listOf(
                    "tvm.Slice",
                    ownerAddressBase64
                ))
            ).successOr { return it }
            val walletAddressCellMap = requireNotNull(
                response.stack[0][1] as? Map<*, *>,
                lazyMessage = { "Can not parse response" },
            )
            val walletAddressCellBase64 = requireNotNull(
                walletAddressCellMap["bytes"] as? String,
                lazyMessage = { "Can not parse response" },
            )
            val walletAddressBoc = BagOfCells(walletAddressCellBase64.decodeBase64Bytes())
            val walletMsgAddress = MsgAddressInt.tlbCodec().loadTlb(walletAddressBoc.roots[0])

            Result.Success(MsgAddressInt.toString(address = walletMsgAddress, userFriendly = true))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getJettonBalance(jettonWalletAddress: String): Result<BigInteger> {
        return try {
            val response = runGetMethod(
                contractAddress = jettonWalletAddress,
                method = GET_WALLET_DATA_METHOD,
                stack = emptyList()
            ).successOr { return it }
            if (response.exitCode == -13) {
                return Result.Success(BigInteger.ZERO)
            }
            val balanceHex = requireNotNull(
                response.stack[0][1] as? String,
                lazyMessage = { "Can not parse response" },
            )

            Result.Success(BigInteger(balanceHex.removePrefix(HEX_PREFIX), 16))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private suspend fun runGetMethod(
        contractAddress: String,
        method: String,
        stack: List<List<String>>
    ): Result<TonRunGetMethodResponse> {
        return try {
            val response = api.post(
                TonProviderMethod.RunGetMethod(
                    contractAddress = contractAddress,
                    method = method,
                    stack = stack
                ).asRequestBody()
            )
            val runGetMethodResponse = requireNotNull(
                value = runGetMethodAdapter.fromJson(response.string()),
                lazyMessage = { "Can not parse response" },
            )
            if (runGetMethodResponse.ok) {
                Result.Success(runGetMethodResponse.result)
            } else {
                val error = BlockchainSdkError.Ton.Api(
                    code = runGetMethodResponse.code ?: 0,
                    message = runGetMethodResponse.error.orEmpty(),
                )
                Result.Failure(error)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    companion object {
        private const val GET_WALLET_ADDRESS_METHOD = "get_wallet_address"
        private const val GET_WALLET_DATA_METHOD = "get_wallet_data"
    }
}