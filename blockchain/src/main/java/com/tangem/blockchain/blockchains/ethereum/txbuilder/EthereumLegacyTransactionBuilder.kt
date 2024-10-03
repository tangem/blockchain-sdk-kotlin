package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.transactions.encode
import org.kethereum.model.Address
import org.kethereum.model.createTransactionWithDefaults
import java.math.BigInteger

/**
 * Ethereum legacy transaction builder
 *
 * @property wallet wallet
 */
internal class EthereumLegacyTransactionBuilder(
    private val wallet: Wallet,
) : EthereumTransactionBuilder(wallet = wallet) {

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
            walletPublicKey = decompressedPublicKey.sliceArray(1..PUBLIC_KEY_SIZE),
            blockchain = blockchain,
        )
    }

    override fun buildDummyTransactionForL1(
        amount: Amount,
        destination: String,
        data: String?,
        fee: Fee.Ethereum,
    ): ByteArray {
        val value = amount.value?.toBigInteger() ?: BigInteger.ZERO
        val legacyFee = fee as Fee.Ethereum.Legacy

        return if (data == null) {
            createTransactionWithDefaults(
                from = Address(wallet.address),
                to = Address(destination),
                value = value, // value is not important for dummy transactions
                gasPrice = legacyFee.gasPrice,
                gasLimit = legacyFee.gasLimit,
                nonce = BigInteger.ONE,
            )
        } else {
            createTransactionWithDefaults(
                from = Address(wallet.address),
                to = Address(destination),
                value = value, // value is not important for dummy transactions
                gasPrice = legacyFee.gasPrice,
                gasLimit = legacyFee.gasLimit,
                nonce = BigInteger.ONE,
                input = data.removePrefix(HEX_PREFIX).hexToBytes(),
            )
        }
            .encode()
    }

    private companion object {
        const val PUBLIC_KEY_SIZE = 64
    }
}