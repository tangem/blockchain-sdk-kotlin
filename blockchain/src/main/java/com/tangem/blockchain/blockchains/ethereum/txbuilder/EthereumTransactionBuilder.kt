package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.tangem.blockchain.blockchains.quai.QuaiBasedOnEthTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
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

    /**
     * Build dummy transaction for getting l1 fee
     *
     * @param amount      amount
     * @param destination destination
     * @param data        transaction hash
     * @param fee         ethereum fee
     */
    abstract fun buildDummyTransactionForL1(
        amount: Amount,
        destination: String,
        data: String?,
        fee: Fee.Ethereum,
    ): ByteArray

    companion object {

        /** Create [EthereumTransactionBuilder] using [Wallet] */
        fun create(wallet: Wallet): EthereumTransactionBuilder {
            return when (wallet.blockchain) {
                Blockchain.Quai,
                Blockchain.QuaiTestnet,
                -> QuaiBasedOnEthTransactionBuilder(wallet)
                else -> EthereumTWTransactionBuilder(wallet)
            }
        }
    }
}