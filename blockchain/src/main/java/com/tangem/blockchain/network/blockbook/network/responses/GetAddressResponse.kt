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
        val vin: List<Vin>,
    ) {

        data class Vin(val addresses: List<String>)

        data class Vout(
            val addresses: List<String>,
            val hex: String,
            val value: String,
        )
    }
}