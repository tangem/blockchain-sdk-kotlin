package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.toDecompressedPublicKey
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
    walletPublicKey: ByteArray,
    private val blockchain: Blockchain,
) {
    private val walletPublicKey: ByteArray = walletPublicKey.toDecompressedPublicKey().sliceArray(1..64)

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

        val recId = ecdsaSignature.determineRecId(
            transactionToSign.hash,
            PublicKey(walletPublicKey)
        )
        val chainId = blockchain.getChainId()
            ?: throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
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
    BscMainnet(56),
    EthereumClassicMainnet(61),
    EthereumClassicTestnet(6),
    BscTestnet(97),
    Gnosis(100),
    Geth_private_chains(1337),
    Avalanche(43114),
    AvalancheTestnet(43113),
    Arbitrum(42161),
    ArbitrumTestnet(421611),
    Polygon(137),
    PolygonTestnet(80001),
    Fantom(250),
    FantomTestnet(4002),
    EthereumFair(513100),
    EthereumPow(10001),
    EthereumPowTestnet(10002),
    ;
}
