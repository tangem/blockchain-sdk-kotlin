package com.tangem.blockchain.blockchains.kaspa.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KaspaBalanceResponse(
    var balance: Long? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaUnspentOutputResponse(
    var outpoint: KaspaOutpoint? = null,
    var utxoEntry: KaspaUtxoEntry? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaSendTransactionResponse(
    var transactionId: String? = null,
    var error: String? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaOutpoint(
    var transactionId: String? = null,
    var index: Long? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaUtxoEntry(
    var amount: String? = null,
    var scriptPublicKey: KaspaScriptPublicKeyResponse? = null,
)

@JsonClass(generateAdapter = true)
data class KaspaScriptPublicKeyResponse(
    var scriptPublicKey: String? = null,
)
