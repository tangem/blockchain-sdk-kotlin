package com.tangem.blockchain.transactionhistory.blockchains.solana.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import org.p2p.solanaj.rpc.RpcException

internal class SolanaTransactionHistoryApi(
    private val rpcClient: SolanaRpcClient,
) {

    private val moshi = Moshi.Builder()
        .add(SolanaAccountKeyAdapter())
        .add(SolanaInstructionAdapter())
        .build()

    private val signaturesAdapter = moshi.adapter<SolanaRpcResponse<List<SolanaSignatureInfo>>>(
        Types.newParameterizedType(
            SolanaRpcResponse::class.java,
            Types.newParameterizedType(List::class.java, SolanaSignatureInfo::class.java),
        ),
    )

    private val transactionAdapter = moshi.adapter<SolanaRpcResponse<SolanaTransactionResponse>>(
        Types.newParameterizedType(
            SolanaRpcResponse::class.java,
            SolanaTransactionResponse::class.java,
        ),
    )

    private val tokenAccountsAdapter = moshi.adapter<SolanaRpcResponse<SolanaTokenAccounts>>(
        Types.newParameterizedType(
            SolanaRpcResponse::class.java,
            SolanaTokenAccounts::class.java,
        ),
    )

    fun getSignaturesForAddress(address: String, limit: Int, before: String? = null): List<SolanaSignatureInfo> {
        val params = buildList {
            add(address)
            add(
                buildMap {
                    put("limit", limit)
                    put("commitment", "finalized")
                    if (before != null) put("before", before)
                },
            )
        }
        val raw = rpcClient.call("getSignaturesForAddress", params)
        val response = signaturesAdapter.fromJson(raw)
            ?: throw RpcException("Failed to parse getSignaturesForAddress response")
        response.error?.let {
            throw RpcException("RPC error ${it.code}: ${it.message}")
        }
        return response.result.orEmpty()
    }

    fun getTokenAccountsByOwner(owner: String, mint: String): String? {
        val params = buildList {
            add(owner)
            add(mapOf("mint" to mint))
            add(mapOf("encoding" to "jsonParsed"))
        }
        val raw = rpcClient.call("getTokenAccountsByOwner", params)
        val response = tokenAccountsAdapter.fromJson(raw)
            ?: throw RpcException("Failed to parse getTokenAccountsByOwner response")
        response.error?.let {
            throw RpcException("RPC error ${it.code}: ${it.message}")
        }
        return response.result?.value?.firstOrNull()?.pubkey
    }

    fun getTransaction(signature: String): SolanaTransactionResponse? {
        val params = buildList {
            add(signature)
            add(
                mapOf(
                    "encoding" to "jsonParsed",
                    "maxSupportedTransactionVersion" to 0,
                ),
            )
        }
        val raw = rpcClient.call("getTransaction", params)
        val response = transactionAdapter.fromJson(raw)
            ?: throw RpcException("Failed to parse getTransaction response")
        response.error?.let {
            throw RpcException("RPC error ${it.code}: ${it.message}")
        }
        return response.result
    }
}