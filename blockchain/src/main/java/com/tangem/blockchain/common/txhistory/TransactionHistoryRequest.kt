package com.tangem.blockchain.common.txhistory

private const val DEFAULT_PAGING_SIZE = 20

data class TransactionHistoryRequest(
    val address: String,
    val page: Page,
    val filterType: FilterType,
) {

    data class Page(val number: Int, val size: Int = DEFAULT_PAGING_SIZE)

    sealed class FilterType {
        object Coin : FilterType()
        data class Contract(val address: String) : FilterType()
    }
}
