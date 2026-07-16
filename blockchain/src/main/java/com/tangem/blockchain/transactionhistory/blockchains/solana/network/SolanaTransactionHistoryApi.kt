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

    private val transactionsForAddressAdapter = moshi.adapter<SolanaRpcResponse<SolanaTransactionsForAddress>>(
        Types.newParameterizedType(
            SolanaRpcResponse::class.java,
            SolanaTransactionsForAddress::class.java,
        ),
    )

    private val tokenAccountsAdapter = moshi.adapter<SolanaRpcResponse<SolanaTokenAccounts>>(
        Types.newParameterizedType(
            SolanaRpcResponse::class.java,
            SolanaTokenAccounts::class.java,
        ),
    )

    fun getTransactionsForAddress(
        address: String,
        limit: Int,
        paginationToken: String? = null,
    ): SolanaTransactionsForAddress {
        val config = buildMap {
            put("transactionDetails", "full")
            put("sortOrder", "desc")
            put("commitment", "finalized")
            put("encoding", "jsonParsed")
            put("maxSupportedTransactionVersion", 0)
            put("limit", limit)
            if (paginationToken != null) put("paginationToken", paginationToken)
        }
        val params = listOf(address, config)
        val raw = rpcClient.call("getTransactionsForAddress", params)
        val response = transactionsForAddressAdapter.fromJson(raw)
            ?: throw RpcException("Failed to parse getTransactionsForAddress response")
        response.error?.let {
            throw RpcException("RPC error ${it.code}: ${it.message}")
        }
        return response.result ?: SolanaTransactionsForAddress(data = emptyList(), paginationToken = null)
    }

    /**
     * Resolves the wallet's on-chain token account (ATA) for [mint] via `getTokenAccountsByOwner`.
     * The `mint` filter matches the account regardless of the owning token program (SPL Token or Token-2022).
     * Returns `null` if the token account doesn't exist yet (no history available).
     */
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
}