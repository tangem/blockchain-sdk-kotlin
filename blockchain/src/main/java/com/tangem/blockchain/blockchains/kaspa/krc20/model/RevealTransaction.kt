package com.tangem.blockchain.blockchains.kaspa.krc20.model

import com.tangem.blockchain.blockchains.kaspa.KaspaTransaction

internal data class RevealTransaction(
    val transaction: KaspaTransaction,
    val hashes: List<ByteArray>,
    val redeemScript: RedeemScript,
)