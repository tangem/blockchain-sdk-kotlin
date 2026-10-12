package com.tangem.blockchain.transactionhistory.models

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page

/**
 * @property gaslessFeeAddresses addresses that collect the fee of a gaslessly sent transaction, known to the caller
 *                               rather than to the SDK. A transfer the wallet made to one of them is what the
 *                               transaction cost its owner, the network fee having been paid by the relayer.
 */
data class TransactionHistoryRequest(
    val address: String,
    val decimals: Int,
    val page: Page,
    val pageSize: Int,
    val filterType: FilterType,
    val gaslessFeeAddresses: Set<String> = emptySet(),
) {

    val pageToLoad: String?
        get() = when (page) {
            Page.Initial -> null
            is Page.Next -> page.value
            Page.LastPage -> error("EOF reached. No data to load")
        }

    sealed class FilterType {
        data object Coin : FilterType()
        data class Contract(val tokenInfo: Token) : FilterType()
    }
}