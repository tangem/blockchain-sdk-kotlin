package com.tangem.blockchain.blockchains.hedera

class HederaTokenAddressConverter {

    fun convertToTokenId(address: String): String {
        return HederaUtils.createTokenId(address).toString()
    }
}