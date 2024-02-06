@file:Suppress("EnumEntryNameCase")

package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.toDecompressedPublicKey
import java.math.BigInteger

@Suppress("LongParameterList")
class EthereumTransactionBuilder(
    walletPublicKey: ByteArray,
    private val blockchain: Blockchain,
) {
    private val walletPublicKey: ByteArray = walletPublicKey.toDecompressedPublicKey().sliceArray(1..64)

    fun buildToSign(transactionData: TransactionData, nonce: BigInteger?): CompiledEthereumTransaction? {
        return EthereumUtils.buildTransactionToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
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
            gasLimit = gasLimit,
        )
    }

    fun buildSetSpendLimitToSign(
        processorContractAddress: String,
        cardAddress: String,
        amount: Amount,
        transactionFee: Amount?,
        gasLimit: BigInteger?,
        nonce: BigInteger?,
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildSetSpendLimitToSign(
            processorContractAddress,
            cardAddress,
            amount,
            transactionFee,
            blockchain,
            gasLimit,
            nonce,
        )
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
            processorContractAddress,
            cardAddress,
            otp,
            otpCounter,
            transactionFee,
            blockchain,
            gasLimit,
            nonce,
        )
    }

    fun buildSetWalletToSign(
        processorContractAddress: String,
        cardAddress: String,
        transactionFee: Amount?,
        gasLimit: BigInteger?,
        nonce: BigInteger,
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildSetWalletToSign(
            processorContractAddress,
            cardAddress,
            transactionFee,
            blockchain,
            gasLimit,
            nonce,
        )
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
        nonceValue: BigInteger,
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
            nonce = nonceValue,
        )
    }

    fun buildTransferFromToSign(
        transactionData: TransactionData,
        nonce: BigInteger?,
        gasLimit: BigInteger?,
    ): CompiledEthereumTransaction? {
        return EthereumUtils.buildTransferFromToSign(
            transactionData = transactionData,
            nonce = nonce,
            blockchain = blockchain,
            gasLimit = gasLimit,
        )
    }

    fun buildToSend(signature: ByteArray, transactionToSign: CompiledEthereumTransaction): ByteArray {
        return EthereumUtils.prepareTransactionToSend(signature, transactionToSign, walletPublicKey, blockchain)
    }
}

enum class Chain(val id: Int, val blockchain: Blockchain?) {
    Mainnet(id = 1, blockchain = Blockchain.Ethereum),
    Goerli(id = 5, blockchain = Blockchain.EthereumTestnet),
    Morden(id = 2, blockchain = null),
    Ropsten(id = 3, blockchain = null),
    Kovan(id = 42, blockchain = null),
    RskMainnet(id = 30, blockchain = Blockchain.RSK),
    RskTestnet(id = 31, blockchain = null),
    BscMainnet(id = 56, blockchain = Blockchain.BSC),
    BscTestnet(id = 97, blockchain = Blockchain.BSCTestnet),
    EthereumClassicMainnet(id = 61, blockchain = Blockchain.EthereumClassic),
    EthereumClassicTestnet(id = 6, blockchain = Blockchain.EthereumClassicTestnet),
    Gnosis(id = 100, blockchain = Blockchain.Gnosis),
    Geth_private_chains(id = 1337, blockchain = null),
    Avalanche(id = 43114, blockchain = Blockchain.Avalanche),
    AvalancheTestnet(id = 43113, blockchain = Blockchain.AvalancheTestnet),
    Arbitrum(id = 42161, blockchain = Blockchain.Arbitrum),
    ArbitrumTestnet(id = 421613, blockchain = Blockchain.ArbitrumTestnet),
    Polygon(id = 137, blockchain = Blockchain.Polygon),
    PolygonTestnet(id = 80001, blockchain = Blockchain.PolygonTestnet),
    Fantom(id = 250, blockchain = Blockchain.Fantom),
    FantomTestnet(id = 4002, blockchain = Blockchain.FantomTestnet),
    Optimism(id = 10, blockchain = Blockchain.Optimism),
    OptimismTestnet(id = 420, blockchain = Blockchain.OptimismTestnet),
    EthereumFair(id = 513100, blockchain = Blockchain.EthereumFair),
    EthereumPow(id = 10001, blockchain = Blockchain.EthereumPow),
    EthereumPowTestnet(id = 10002, blockchain = Blockchain.EthereumPowTestnet),
    Kava(id = 2222, blockchain = Blockchain.Kava),
    KavaTestnet(id = 2221, blockchain = Blockchain.KavaTestnet),
    Telos(id = 40, blockchain = Blockchain.Telos),
    TelosTestnet(id = 41, blockchain = Blockchain.TelosTestnet),
    Cronos(id = 25, blockchain = Blockchain.Cronos),
    OctaSpace(id = 800001, blockchain = Blockchain.OctaSpace),
    OctaSpaceTestnet(id = 800002, blockchain = Blockchain.OctaSpaceTestnet),
    Decimal(id = 75, blockchain = Blockchain.Decimal),
    DecimalTestnet(id = 202020, blockchain = Blockchain.DecimalTestnet),
    Xdc(id = 50, blockchain = Blockchain.XDC),
    XdcTestnet(id = 51, blockchain = Blockchain.XDCTestnet),
}
