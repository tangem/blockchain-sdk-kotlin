package com.tangem.blockchain.common.pagination

sealed class Page {
    object Initial : Page()
    data class Next(val value: String) : Page()
    object LastPage : Page()
}