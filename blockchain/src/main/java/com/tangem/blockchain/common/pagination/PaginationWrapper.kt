package com.tangem.blockchain.common.pagination

data class PaginationWrapper<T>(
    val nextPage: Page,
    val items: List<T>,
)