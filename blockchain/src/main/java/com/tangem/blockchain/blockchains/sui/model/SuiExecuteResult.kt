package com.tangem.blockchain.blockchains.sui.model

internal data class SuiExecuteResult(
    val digest: String,
    val isSuccess: Boolean,
)