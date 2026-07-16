package com.tangem.blockchain.common.psbt

/**
 * Decodes a scriptPubKey into the chain's native address string.
 * Returns null for non-standard scripts (e.g. OP_RETURN) that carry no address.
 */
internal interface PsbtAddressCodec {
    fun scriptToAddress(scriptPubKey: ByteArray): String?
}