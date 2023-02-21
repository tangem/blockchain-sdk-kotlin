package com.tangem.blockchain.blockchains.polkadot.network

import com.fasterxml.jackson.databind.ObjectMapper
import com.tangem.blockchain.blockchains.polkadot.polkaj.extensions.amountUnits
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import io.emeraldpay.polkaj.api.PolkadotApi
import io.emeraldpay.polkaj.api.RpcCallAdapter
import io.emeraldpay.polkaj.api.RpcCoder
import io.emeraldpay.polkaj.api.StandardCommands
import io.emeraldpay.polkaj.apihttp.JavaRetrofitAdapter
import io.emeraldpay.polkaj.json.jackson.PolkadotModule
import io.emeraldpay.polkaj.scaletypes.AccountInfo
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.tx.AccountRequests
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.ByteData
import io.emeraldpay.polkaj.types.DotAmount
import io.emeraldpay.polkaj.types.Hash256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class PolkadotNetworkService(
    private val network: SS58Type.Network,
    val host: String
) {

    private val polkadotApi: PolkadotApi = PolkadotApi.Builder().rpcCallAdapter(rpcCallAdapter(host)).build()
    private val polkadotProvider: PolkadotJsonRpcProvider = PolkadotJsonRpcProvider(host)
    private val commands = StandardCommands.getInstance()

    suspend fun getBalance(address: Address): Result<DotAmount> = withContext(Dispatchers.IO) {
        val accountInfo = getAccountInfo(address).successOr { return@withContext it }

        val dotAmount = accountInfo?.data?.free ?: DotAmount.from(0, network.amountUnits)
        Result.Success(dotAmount)
    }

    suspend fun getAccountInfo(address: Address): Result<AccountInfo?> = withContext(Dispatchers.IO) {
        try {
            val info = AccountRequests.balanceOf(address).execute(polkadotApi).get()
            Result.Success(info)
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    suspend fun getFee(builtTransaction: ByteArray): Result<BigDecimal> = withContext(Dispatchers.IO) {
        try {
            val fee = polkadotProvider.getFee(
                transaction = builtTransaction,
                decimals = network.amountUnits.main.decimals
            )
            Result.Success(fee)
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    suspend fun sendTransaction(builtTransaction: ByteArray): Result<Hash256> = withContext(Dispatchers.IO) {
        try {
            val txId = polkadotApi.execute(commands.authorSubmitExtrinsic(ByteData(builtTransaction))).get()
            Result.Success(txId)
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    suspend fun extrinsicContext(address: Address): Result<ExtrinsicContext> = withContext(Dispatchers.IO) {
        try {
            Result.Success(polkadotApi.autoContext(address))
        } catch (ex: Exception) {
            Result.Failure(BlockchainSdkError.Polkadot.Api(ex))
        }
    }

    companion object {
        fun network(blockchain: Blockchain): SS58Type.Network = when (blockchain) {
            Blockchain.Polkadot -> SS58Type.Network.POLKADOT
            Blockchain.PolkadotTestnet -> SS58Type.Network.WESTEND
            Blockchain.Kusama -> SS58Type.Network.KUSAMA
            else -> throw IllegalArgumentException()
        }

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