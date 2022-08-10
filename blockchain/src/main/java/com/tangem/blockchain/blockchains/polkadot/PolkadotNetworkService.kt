package com.tangem.blockchain.blockchains.polkadot

import com.fasterxml.jackson.databind.ObjectMapper
import com.tangem.blockchain.blockchains.polkadot.polkaj.setSignedSignature
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import io.emeraldpay.polkaj.api.PolkadotApi
import io.emeraldpay.polkaj.api.RpcCallAdapter
import io.emeraldpay.polkaj.api.RpcCoder
import io.emeraldpay.polkaj.api.StandardCommands
import io.emeraldpay.polkaj.apihttp.RetrofitAdapter
import io.emeraldpay.polkaj.json.jackson.PolkadotModule
import io.emeraldpay.polkaj.scaletypes.AccountInfo
import io.emeraldpay.polkaj.tx.AccountRequests
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.DotAmount
import io.emeraldpay.polkaj.types.DotAmountFormatter
import io.emeraldpay.polkaj.types.Hash256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
[REDACTED_AUTHOR]
 */
class PolkadotNetworkService(
    private val rpcCallAdapter: RpcCallAdapter
) {
    private val client: PolkadotApi = PolkadotApi.Builder().rpcCallAdapter(rpcCallAdapter).build()
    private val commands = StandardCommands.getInstance()

    suspend fun getBalance(address: Address): Result<DotAmount> {
        val formatter = DotAmountFormatter.autoFormatter()
        val total = AccountRequests.totalIssuance().execute(client).get()
        val balance = AccountRequests.balanceOf(address).execute(client).get()
        return Result.Success(balance.data.free)
    }

    suspend fun accountInfo(address: Address): Result<Any> = withContext(Dispatchers.IO) {
        val info = AccountRequests.balanceOf(address).execute(client).get()
        Result.Success(info)
    }

    suspend fun getFees(): Result<Any> = withContext(Dispatchers.IO) {
        client.execute(commands.paymentQueryInfo())

    }

    suspend fun sendTransaction(): Result<Hash256> = withContext(Dispatchers.IO) {
        val transfer = AccountRequests
            .transfer()
            .runtime()
            .from()
            .to()
            .amount()
            .setSignedSignature()
            .build()

        val txId = client.execute(commands.authorSubmitExtrinsic(transfer.encodeRequest())).get()
        Result.Success(txId)
    }

    private suspend fun getTransactionsInProgressInfo(): Result<Any> = withContext(Dispatchers.IO) {
    }

    companion object {

        fun createRpcCallAdapter(blockchain: Blockchain): RpcCallAdapter {
            val baseUrl = when (blockchain) {
                Blockchain.Polkadot -> "https://rpc.polkadot.io"
                Blockchain.PolkadotTestnet -> "https://westend-rpc.polkadot.io"
//                Blockchain.Kusama -> "https://kusama-rpc.polkadot.io"
                else -> throw IllegalArgumentException()
            }
            val rpcCoder = RpcCoder(ObjectMapper().apply { registerModule(PolkadotModule()) })

            return RetrofitAdapter(baseUrl, BlockchainSdkRetrofitBuilder.okHttpClient, rpcCoder)
        }
    }
}