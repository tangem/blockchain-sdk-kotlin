package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page

data class TransactionHistoryRequest(
    val address: String,
    val decimals: Int,
    val page: Page,
    val pageSize: Int,
    val filterType: FilterType,
) {

    val pageToLoad: String?
        get() = when (page) {
            Page.Initial -> null
            is Page.Next -> page.value
            Page.LastPage -> error("EOF reached. No data to load")
        }

    sealed class FilterType {
        object Coin : FilterType()
        data class Contract(val tokenInfo: Token) : FilterType()
    }
}