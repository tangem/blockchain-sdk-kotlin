package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.toDecompressedPublicKey
import java.math.BigInteger

class EthereumTransactionBuilder(
    walletPublicKey: ByteArray,
    private val blockchain: Blockchain,
) {
    private val walletPublicKey: ByteArray = walletPublicKey.toDecompressedPublicKey().sliceArray(1..64)

    fun buildToSign(transactionData: TransactionData, nonce: BigInteger?, gasLimit: BigInteger?): CompiledEthereumTransaction? {
        return EthereumUtils.buildTransactionToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
            gasLimit = gasLimit
        )
    }

    fun buildApproveToSign(
        transactionData: TransactionData,
        nonce: BigInteger?,
        gasLimit: BigInteger?,
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildApproveToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
            gasLimit = gasLimit
        )
    }

    fun buildSetSpendLimitToSign(
        processorContractAddress: String,
        cardAddress: String,
        amount: Amount,
        transactionFee: Amount?,
        gasLimit: BigInteger?, nonce: BigInteger?,
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildSetSpendLimitToSign(
            processorContractAddress, cardAddress, amount, transactionFee, blockchain, gasLimit, nonce)
    }

    fun buildInitOTPToSign(
        processorContractAddress: String,
        cardAddress: String,
        otp: ByteArray,
        otpCounter: Int,
        transactionFee: Amount?,
        gasLimit: BigInteger?,
        nonce: BigInteger?,
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildInitOTPToSign(
            processorContractAddress, cardAddress, otp, otpCounter, transactionFee, blockchain, gasLimit, nonce)
    }

    fun buildSetWalletToSign(
        processorContractAddress: String,
        cardAddress: String,
        transactionFee: Amount?,
        gasLimit: BigInteger?,
        nonce: BigInteger
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildSetWalletToSign(
            processorContractAddress, cardAddress, transactionFee, blockchain, gasLimit, nonce)
    }

    fun buildProcessToSign(
        processorContractAddress: String,
        processorAddress: String,
        cardAddress: String,
        amount: Amount,
        transactionFee: Amount,
        otp: ByteArray,
        otpCounter: Int,
        gasLimit: BigInteger?,
        nonceValue: BigInteger
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildProcessToSign(
            processorContractAddress = processorContractAddress,
            processorAddress = processorAddress,
            cardAddress = cardAddress,
            amount = amount,
            transactionFee = transactionFee,
            otp = otp,
            otpCounter = otpCounter,
            blockchain = blockchain,
            gasLimit = gasLimit,
            nonce = nonceValue
        )
    }

    fun buildTransferFromToSign(transactionData: TransactionData, nonce: BigInteger?, gasLimit: BigInteger?): CompiledEthereumTransaction? {
        return EthereumUtils.buildTransferFromToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
            gasLimit = gasLimit
        )
    }

    fun buildToSend(signature: ByteArray, transactionToSign: CompiledEthereumTransaction): ByteArray {
        return EthereumUtils.prepareTransactionToSend(signature, transactionToSign, walletPublicKey, blockchain)
    }
}

enum class Chain(
    val id: Int,
    val blockchain: Blockchain?,
) {
    Mainnet(1, Blockchain.Ethereum),
    Goerli(5, Blockchain.EthereumTestnet),
    Morden(2, null),
    Ropsten(3, null),
    Kovan(42, null),
    RskMainnet(30, Blockchain.RSK),
    RskTestnet(31, null),
    BscMainnet(56, Blockchain.BSC),
    BscTestnet(97, Blockchain.BSCTestnet),
    EthereumClassicMainnet(61, Blockchain.EthereumClassic),
    EthereumClassicTestnet(6, Blockchain.EthereumClassicTestnet),
    Gnosis(100, Blockchain.Gnosis),
    Geth_private_chains(1337, null),
    Avalanche(43114, Blockchain.Avalanche),
    AvalancheTestnet(43113, Blockchain.AvalancheTestnet),
    Arbitrum(42161, Blockchain.Arbitrum),
    ArbitrumTestnet(421613, Blockchain.ArbitrumTestnet),
    Polygon(137, Blockchain.Polygon),
    PolygonTestnet(80001, Blockchain.PolygonTestnet),
    Fantom(250, Blockchain.Fantom),
    FantomTestnet(4002, Blockchain.FantomTestnet),
    Optimism(10, Blockchain.Optimism),
    OptimismTestnet(420, Blockchain.OptimismTestnet),
    EthereumFair(513100, Blockchain.EthereumFair),
    EthereumPow(10001, Blockchain.EthereumPow),
    EthereumPowTestnet(10002, Blockchain.EthereumPowTestnet),
    Kava(2222, Blockchain.Kava),
    KavaTestnet(2221, Blockchain.KavaTestnet),
    Cronos(25, Blockchain.Cronos),
}
