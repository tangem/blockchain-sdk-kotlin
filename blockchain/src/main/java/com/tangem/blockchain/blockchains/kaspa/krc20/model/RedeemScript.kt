package com.tangem.blockchain.blockchains.kaspa.krc20.model

internal data class RedeemScript(
    val publicKey: ByteArray,
    val envelope: Envelope,
)