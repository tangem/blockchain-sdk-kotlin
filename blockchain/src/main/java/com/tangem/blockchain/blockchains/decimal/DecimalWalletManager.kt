package com.tangem.blockchain.blockchains.decimal

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

internal class DecimalWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider) {

    override suspend fun getFee(
        amount: Amount,
        destination: String,
        smartContract: SmartContractCallData?,
    ): Result<TransactionFee> {
        return super.getFee(amount, convertAddress(destination), smartContract)
    }

    override suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        smartContract: SmartContractCallData?,
    ): Result<TransactionFee> {
        return super.getFeeInternal(amount, convertAddress(destination), smartContract)
    }

    override suspend fun getGasLimit(amount: Amount, destination: String): Result<BigInteger> {
        return super.getGasLimit(amount, convertAddress(destination))
    }

    override suspend fun getGasLimit(
        amount: Amount,
        destination: String,
        smartContract: SmartContractCallData,
    ): Result<BigInteger> {
        return super.getGasLimit(amount, convertAddress(destination), smartContract)
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()
        return super.send(convertTransactionDataAddress(transactionData), signer)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, EthereumCompiledTxInfo>> {
        transactionData.requireUncompiled()
        return super.sign(convertTransactionDataAddress(transactionData), signer)
    }

    private fun convertTransactionDataAddress(transactionData: TransactionData.Uncompiled) = transactionData.copy(
        destinationAddress = convertAddress(transactionData.destinationAddress),
    )

    private fun convertAddress(destinationAddress: String): String {
        return DecimalAddressService.convertDelAddressToDscAddress(destinationAddress)
    }
}