package com.tangem.blockchain.blockchains.ethereum.ens

import com.tangem.blockchain.extensions.Result

/**
 * Prepares data for ENS contract calls.
 * https://docs.ens.domains/resolution/names/#algorithm-1
 * https://docs.ens.domains/resolvers/universal
 */
internal interface ENSNameProcessor {

    fun getNamehash(name: String): Result<ByteArray>

    fun encode(name: String): Result<ByteArray>
}