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

    fun getTransactionsForAddress(
        address: String,
        limit: Int,
        paginationToken: String? = null,
        tokenAccountsFilter: TokenAccountsFilter = TokenAccountsFilter.Default,
    ): SolanaTransactionsForAddress {
        val config = buildMap {
            put("transactionDetails", "full")
            put("sortOrder", "desc")
            put("commitment", "finalized")
            put("encoding", "jsonParsed")
            put("maxSupportedTransactionVersion", 0)
            put("limit", limit)
            if (paginationToken != null) put("paginationToken", paginationToken)
            tokenAccountsFilter.asFilters()?.let { put("filters", it) }
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
     * Restricts the returned transactions to those that changed the wallet's token accounts.
     * Used for SPL token history; coin history uses [Default] (no filter).
     */
    enum class TokenAccountsFilter {
        Default,
        BalanceChanged,
        ;

        fun asFilters(): Map<String, String>? = when (this) {
            Default -> null
            BalanceChanged -> mapOf("tokenAccounts" to "balanceChanged")
        }
    }
}