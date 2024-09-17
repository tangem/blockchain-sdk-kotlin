package com.tangem.blockchain.blockchains.ethereum.txbuilder

import org.kethereum.model.Transaction

/** Ethereum compiled transaction */
sealed interface EthereumCompiledTxInfo {

    /** Hash of transaction */
    val hash: ByteArray

    /**
     * Compiled transaction info for legacy way of building transaction
     *
     * @property hash        hash
     * @property transaction transaction
     */
    data class Legacy(
        override val hash: ByteArray,
        val transaction: Transaction,
    ) : EthereumCompiledTxInfo

    /**
     * Compiled transaction info for building transaction by TW
     *
     * @property hash hash
     */
    data class TWInfo(override val hash: ByteArray) : EthereumCompiledTxInfo
}