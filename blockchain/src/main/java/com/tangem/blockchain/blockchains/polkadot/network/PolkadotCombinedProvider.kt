package com.tangem.blockchain.blockchains.polkadot.network

import com.fasterxml.jackson.databind.ObjectMapper
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.blockchains.polkadot.extensions.toBigDecimal
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
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
import io.emeraldpay.polkaj.tx.AccountRequests.AddressBalance
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.ByteData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

internal class PolkadotCombinedProvider(
    override val baseUrl: String,
    private val blockchain: Blockchain,
    credentials: Map<String, String>? = null,
) : PolkadotNetworkProvider {

    private val decimals by lazy { blockchain.decimals() }

    private val polkadotApi: PolkadotApi = PolkadotApi.Builder()
        .rpcCallAdapter(rpcCallAdapter(baseUrl, credentials))
        .build()
    private val polkadotProvider: PolkadotJsonRpcProvider = PolkadotJsonRpcProvider(baseUrl, credentials)
    private val commands = StandardCommands.getInstance()
    private val ss58Network = PolkadotAddressService(blockchain).ss58Network

    override suspend fun getBalance(address: String): Result<BigDecimal> = withContext(Dispatchers.IO) {
        val accountInfo = getAccountInfo(Address.from(address)).successOr { return@withContext it }

        val amount = accountInfo?.data?.free?.toBigDecimal(decimals) ?: BigDecimal.ZERO
        Result.Success(amount)
    }

    private suspend fun getAccountInfo(address: Address): Result<AccountInfo?> = withContext(Dispatchers.IO) {
        try {
            val info = AddressBalance(address, ss58Network).execute(polkadotApi).get()
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

    override suspend fun getLatestBlockHash(): Result<String> {
        return try {
            val latestBlock = polkadotProvider.getLatestBlockHash().successOr { return it }
            Result.Success(latestBlock)
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    override suspend fun getBlockNumber(blockHash: String): Result<BigInteger> {
        return try {
            val latestBlock = polkadotProvider.getBlockNumber(blockHash).successOr { return it }
            Result.Success(latestBlock)
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

        private fun rpcCallAdapter(baseUrl: String, credentials: Map<String, String>?): RpcCallAdapter {
            val interceptors = if (credentials != null) listOf(AddHeaderInterceptor(credentials)) else emptyList()
            val okHttpClient = BlockchainSdkRetrofitBuilder.build(interceptors)
            val rpcCoder = RpcCoder(ObjectMapper().apply { registerModule(PolkadotModule()) })

            return JavaRetrofitAdapter(baseUrl, okHttpClient, rpcCoder)
        }
    }
}

internal fun PolkadotApi.autoContext(address: Address): ExtrinsicContext {
    return ExtrinsicContext.newAutoBuilder(address, this).get().build()
}