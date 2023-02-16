package com.tangem.blockchain.network.blockbook.network.responses

data class GetAddressResponse(
    val address: String,
    val balance: String,
    val transactions: List<Transaction>?,
    val txs: Int,
    val unconfirmedBalance: String,
    val unconfirmedTxs: Int
) {

    data class Transaction(
        val blockHash: String?,
        val blockHeight: Int,
        val blockTime: Int,
        val confirmations: Int,
        val fees: String,
        val hex: String,
        val lockTime: Int,
        val size: Int,
        val txid: String,
        val value: String,
        val valueIn: String,
        val version: Int,
        val vin: List<Vin>,
        val vout: List<Vout>,
        val vsize: Int
    ) {

        data class Vin(
            val addresses: List<String>,
            val isAddress: Boolean,
            val isOwn: Boolean?,
            val n: Int,
            val sequence: Long?,
            val txid: String,
            val value: String?,
            val vout: Int?
        )

        data class Vout(
            val addresses: List<String>,
            val hex: String,
            val isAddress: Boolean,
            val isOwn: Boolean?,
            val n: Int,
            val spent: Boolean?,
            val value: String
        )
    }
}