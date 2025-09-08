package com.tangem.blockchain.blockchains.ethereum.converters

abstract class EthereumResponseConverter {

    // Each "word" in Ethereum ABI is 32 bytes, which equals 64 hex characters
    val WORD_BYTES = 32
    val HEX_CHARS_PER_BYTE = 2
    val WORD_HEX_LENGTH = WORD_BYTES * HEX_CHARS_PER_BYTE // 64
}