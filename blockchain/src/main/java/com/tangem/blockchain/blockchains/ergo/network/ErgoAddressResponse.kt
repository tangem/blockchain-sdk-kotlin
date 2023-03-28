package com.tangem.blockchain.blockchains.ergo.network


data class ErgoAddressResponse(
    val balance: Long,
    val TransactionsId: List<String>
)
