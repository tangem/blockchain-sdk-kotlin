package com.tangem.blockchain.blockchains.ton.models

data class GetJettonWalletAddressInput(
    val ownerAddress: String,
    val jettonMinterAddress: String,
)