package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.transactions.encodeRLP
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction
import java.math.BigInteger

class EthereumTransactionBuilder(
    private val walletPublicKey: ByteArray, private val blockchain: Blockchain,
) {
    internal var gasLimit = DEFAULT_GAS_LIMIT

    fun buildToSign(transactionData: TransactionData, nonce: BigInteger?): TransactionToSign? {
        return EthereumUtils.buildTransactionToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
            gasLimit = gasLimit
        )
    }

    fun buildToSend(signature: ByteArray, transactionToSign: TransactionToSign): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(transactionToSign.hash, PublicKey(walletPublicKey.sliceArray(1..64)))
        val chainId = EthereumUtils.getChainId(blockchain)
        val v = (recId + 27 + 8 + (chainId * 2)).toBigInteger()
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return transactionToSign.transaction.encodeRLP(signatureData)
    }
}

class TransactionToSign(val transaction: Transaction, val hash: ByteArray)

enum class Chain(val id: Int) {
    Mainnet(1),
    Morden(2),
    Ropsten(3),
    Rinkeby(4),
    RskMainnet(30),
    RskTestnet(31),
    Kovan(42),
    EthereumClassicMainnet(61),
    EthereumClassicTestnet(62),
    Geth_private_chains(1337),
    MaticTestnet(8995);
}