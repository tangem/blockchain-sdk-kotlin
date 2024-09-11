package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet

/**
 * Ethereum legacy transaction builder
 *
 * @param wallet wallet
 */
internal class EthereumLegacyTransactionBuilder(wallet: Wallet) : EthereumTransactionBuilder(wallet = wallet) {

    override fun buildForSign(transaction: TransactionData): EthereumCompiledTxInfo {
        return EthereumUtils.buildTransactionToSign(transactionData = transaction, blockchain = blockchain)
    }

    override fun buildForSend(
        transaction: TransactionData,
        signature: ByteArray,
        compiledTransaction: EthereumCompiledTxInfo,
    ): ByteArray {
        return EthereumUtils.prepareTransactionToSend(
            signature = signature,
            transactionToSign = compiledTransaction as EthereumCompiledTxInfo.Legacy,
            walletPublicKey = publicKey,
            blockchain = blockchain,
        )
    }
}