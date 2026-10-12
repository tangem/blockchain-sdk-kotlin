package com.tangem.blockchain.blockchains.tron.tokenmethods

import com.tangem.blockchain.blockchains.tron.libs.Base58Check

private const val WORD_SIZE = 32
private const val ADDRESS_BODY_SIZE = 20
private const val PADDING_SIZE = WORD_SIZE - ADDRESS_BODY_SIZE - 1
private const val PREFIX_INDEX = PADDING_SIZE
private const val ADDRESS_PREFIX: Byte = 0x41
private const val ZERO_BYTE: Byte = 0

/**
 * Read the `address` argument of a TRC20 call out of its 32-byte word, as the base58 form the address is written
 * in everywhere else.
 *
 * Tron's own tooling encodes the whole 21-byte address, prefix included, while a wallet reusing the EVM encoding
 * pads the 20-byte body instead — both reach the same contract and both are in the wild. The body is identical
 * either way, so the prefix is simply put back on. Returns `null` for a word that holds no address at all.
 */
internal fun ByteArray.decodeTronAddressWord(): String? {
    if (size != WORD_SIZE) return null
    if (take(PADDING_SIZE).any { it != ZERO_BYTE }) return null

    val prefix = this[PREFIX_INDEX]
    if (prefix != ADDRESS_PREFIX && prefix != ZERO_BYTE) return null

    val body = copyOfRange(PREFIX_INDEX + 1, WORD_SIZE)
    if (body.all { it == ZERO_BYTE }) return null

    return Base58Check.bytesToBase58(byteArrayOf(ADDRESS_PREFIX) + body)
}