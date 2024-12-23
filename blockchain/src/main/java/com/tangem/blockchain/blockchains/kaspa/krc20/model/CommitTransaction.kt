package com.tangem.blockchain.blockchains.kaspa.krc20.model

import com.tangem.blockchain.blockchains.kaspa.KaspaTransaction
import com.tangem.blockchain.common.datastorage.BlockchainSavedData

internal data class CommitTransaction(
    val transaction: KaspaTransaction,
    val hashes: List<ByteArray>,
    val redeemScript: RedeemScript,
    val sourceAddress: String,
    val params: BlockchainSavedData.KaspaKRC20IncompleteTokenTransaction,
)