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

enum class Chain(
    val id: Int,
    val blockchain: Blockchain?,
) {
    Mainnet(1, Blockchain.Ethereum),
    Rinkeby(4, Blockchain.EthereumTestnet),
    Morden(2,  null),
    Ropsten(3, null),
    Kovan(42, null),
    RskMainnet(30, Blockchain.RSK),
    RskTestnet(31, null),
    BscMainnet(56, Blockchain.BSC),
    BscTestnet(97, Blockchain.BSCTestnet),
    EthereumClassicMainnet(61, Blockchain.EthereumClassic),
    EthereumClassicTestnet(6, Blockchain.EthereumClassicTestnet),
    Gnosis(100, Blockchain.Gnosis),
    Dash(1111, Blockchain.Dash),
    DashTestnet(2222, Blockchain.DashTestNet),
    Geth_private_chains(1337, null),
    Avalanche(43114, Blockchain.Avalanche),
    AvalancheTestnet(43113, Blockchain.AvalancheTestnet),
    Arbitrum(42161, Blockchain.Arbitrum),
    ArbitrumTestnet(421611, Blockchain.ArbitrumTestnet),
    Polygon(137, Blockchain.Polygon),
    PolygonTestnet(80001, Blockchain.PolygonTestnet),
    Fantom(250, Blockchain.Fantom),
    FantomTestnet(4002, Blockchain.FantomTestnet);
}