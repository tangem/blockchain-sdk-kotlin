package com.tangem.blockchain.network.blockbook.network.responses

data class GetAddressResponse(
    val balance: String,
    val unconfirmedTxs: Int,
    val txs: Int,
    val transactions: List<Transaction>?,
) {

    data class Transaction(
        val txid: String,
        val vout: List<Vout>,
        val confirmations: Int,
        val blockTime: Int,
        val value: String,
    ) {

        data class Vout(val hex: String, val addresses: List<String>)
    }
}