package com.tangem.blockchain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.commands.common.card.Card

fun Card.getToken(): Token? {
    val symbol = this.cardData?.tokenSymbol ?: return null
    val contractAddress = this.cardData?.tokenContractAddress ?: return null
    val decimals = this.cardData?.tokenDecimal ?: return null
    return Token(symbol, contractAddress, decimals)
}

fun Card.getBlockchain(): Blockchain? {
    val blockchainName: String = this.cardData?.blockchainName ?: return null
    return Blockchain.fromId(blockchainName)
}