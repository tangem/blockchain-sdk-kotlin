package com.tangem.blockchain.blockchains.ethereum.txbuilder

import org.kethereum.model.Transaction
import wallet.core.jni.proto.Ethereum

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
     * @property hash  hash
     * @property input transaction input
     */
    data class TWInfo(
        override val hash: ByteArray,
        val input: Ethereum.SigningInput,
    ) : EthereumCompiledTxInfo
}