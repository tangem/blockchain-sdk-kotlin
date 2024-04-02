package com.tangem.blockchain.network.blockbook.config

import com.tangem.blockchain.common.txhistory.TransactionHistoryRequest

sealed class BlockBookRequest {
    data class GetAddress(
        val page: String? = null,
        val pageSize: Int? = null,
        val filterType: TransactionHistoryRequest.FilterType? = null,
    ) : BlockBookRequest() {

        fun params(): String = buildString {
            if (page != null) append("&page=$page")
            if (pageSize != null) append("&pageSize=$pageSize")
            if (filterType != null) append(filterType.toParam())
        }

        private fun TransactionHistoryRequest.FilterType.toParam(): String = when (this) {
            TransactionHistoryRequest.FilterType.Coin -> "&filter=0"
            is TransactionHistoryRequest.FilterType.Contract -> "&contract=${tokenInfo.contractAddress}"
        }
    }

    object GetFee : BlockBookRequest()
    object SendTransaction : BlockBookRequest()
    object GetUTXO : BlockBookRequest()
    data class GetTxById(val txId: String) : BlockBookRequest()
}