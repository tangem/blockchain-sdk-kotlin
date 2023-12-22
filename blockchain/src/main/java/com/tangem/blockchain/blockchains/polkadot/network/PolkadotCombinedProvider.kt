package com.tangem.blockchain.blockchains.polkadot.network

import com.fasterxml.jackson.databind.ObjectMapper
import com.tangem.blockchain.blockchains.polkadot.extensions.toBigDecimal
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.common.extensions.toHexString
import io.emeraldpay.polkaj.api.PolkadotApi
import io.emeraldpay.polkaj.api.RpcCallAdapter
import io.emeraldpay.polkaj.api.RpcCoder
import io.emeraldpay.polkaj.api.StandardCommands
import io.emeraldpay.polkaj.apihttp.JavaRetrofitAdapter
import io.emeraldpay.polkaj.json.jackson.PolkadotModule
import io.emeraldpay.polkaj.scaletypes.AccountInfo
import io.emeraldpay.polkaj.tx.AccountRequests
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.ByteData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 10/06/2022.
 */
class PolkadotCombinedProvider(
    private val decimals: Int,
    override val baseUrl: String,
) : PolkadotNetworkProvider {

    private val polkadotApi: PolkadotApi = PolkadotApi.Builder().rpcCallAdapter(rpcCallAdapter(baseUrl)).build()
    private val polkadotProvider: PolkadotJsonRpcProvider = PolkadotJsonRpcProvider(baseUrl)
    private val commands = StandardCommands.getInstance()

    override suspend fun getBalance(address: String): Result<BigDecimal> = withContext(Dispatchers.IO) {
        val accountInfo = getAccountInfo(Address.from(address)).successOr { return@withContext it }

        val amount = accountInfo?.data?.free?.toBigDecimal(decimals) ?: BigDecimal.ZERO
        Result.Success(amount)
    }

    private suspend fun getAccountInfo(address: Address): Result<AccountInfo?> = withContext(Dispatchers.IO) {
        try {
            val info = AccountRequests.balanceOf(address).execute(polkadotApi).get()
            Result.Success(info)
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    override suspend fun getFee(builtTransaction: ByteArray): Result<BigDecimal> = withContext(Dispatchers.IO) {
        try {
            val fee = polkadotProvider.getFee(
                transaction = builtTransaction,
                decimals = decimals,
            )
            Result.Success(fee)
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    override suspend fun sendTransaction(builtTransaction: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val txId = polkadotApi.execute(commands.authorSubmitExtrinsic(ByteData(builtTransaction))).get()
            Result.Success(txId.bytes.toHexString())
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    override suspend fun extrinsicContext(address: String): Result<ExtrinsicContext> = withContext(Dispatchers.IO) {
        try {
            Result.Success(polkadotApi.autoContext(Address.from(address)))
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    companion object {

        private fun rpcCallAdapter(baseUrl: String): RpcCallAdapter {
            val okHttpClient = BlockchainSdkRetrofitBuilder.build()
            val rpcCoder = RpcCoder(ObjectMapper().apply { registerModule(PolkadotModule()) })

            return JavaRetrofitAdapter(baseUrl, okHttpClient, rpcCoder)
        }
    }
}

internal fun PolkadotApi.autoContext(address: Address): ExtrinsicContext {
    return ExtrinsicContext.newAutoBuilder(address, this).get().build()
}
