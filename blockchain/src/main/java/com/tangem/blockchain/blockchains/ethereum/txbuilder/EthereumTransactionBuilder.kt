package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.common.extensions.toDecompressedPublicKey

/**
 * Ethereum transaction builder
 *
 * @param wallet wallet
 */
abstract class EthereumTransactionBuilder(wallet: Wallet) {

    /** Decompressed wallet public key */
    protected val decompressedPublicKey by lazy { wallet.publicKey.blockchainKey.toDecompressedPublicKey() }

    /** Blockchain */
    protected val blockchain by lazy(wallet::blockchain)

    /**
     * Build for signing transaction
     *
     * @param transaction transaction data
     */
    abstract fun buildForSign(transaction: TransactionData): EthereumCompiledTxInfo

    /**
     * Build for sending transaction
     *
     * @param transaction         transaction data
     * @param signature           signature
     * @param compiledTransaction compiled transaction info
     */
    abstract fun buildForSend(
        transaction: TransactionData,
        signature: ByteArray,
        compiledTransaction: EthereumCompiledTxInfo,
    ): ByteArray

    companion object {

        /** Create [EthereumTransactionBuilder] using [Wallet] */
        fun create(wallet: Wallet): EthereumTransactionBuilder {
            val isEthereumEIP1559Enabled = DepsContainer.blockchainFeatureToggles.isEthereumEIP1559Enabled

            return if (isEthereumEIP1559Enabled && wallet.blockchain.isSupportEIP1559) {
                EthereumTWTransactionBuilder(wallet)
            } else {
                EthereumLegacyTransactionBuilder(wallet)
            }
        }
    }
}