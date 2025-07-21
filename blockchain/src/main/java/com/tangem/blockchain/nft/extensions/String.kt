package com.tangem.blockchain.nft.extensions

fun String.ipfsToHttps(): String {
    return if (startsWith("ipfs://")) {
        replaceFirst("ipfs://", "https://ipfs.io/ipfs/")
    } else {
        this
    }
}

fun String.removeUrlQuery(): String {
    return substringBefore("?")
}