package com.tangem.blockchain.common

data class PaginationWrapper<T>(
    val page: Int,
    val totalPages: Int,
    val itemsOnPage: Int,
    val items: List<T>,
)
