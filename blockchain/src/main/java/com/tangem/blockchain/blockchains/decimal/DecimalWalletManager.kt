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
) : EthereumWalletManager(
    wallet = wallet,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    supportsENS = false,
) {

    override suspend fun getFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee> {
        return super.getFee(amount, convertAddress(destination), callData)
    }

    override suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee> {
        return super.getFeeInternal(amount, convertAddress(destination), callData)
    }

    override suspend fun getGasLimit(amount: Amount, destination: String): Result<BigInteger> {
        return super.getGasLimit(amount, convertAddress(destination))
    }

    override suspend fun getGasLimit(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData,
    ): Result<BigInteger> {
        return super.getGasLimit(amount, convertAddress(destination), callData)
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val uncompiledTransaction = transactionData.requireUncompiled()
        return super.send(convertTransactionDataAddress(uncompiledTransaction), signer)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, EthereumCompiledTxInfo>> {
        val uncompiledTransaction = transactionData.requireUncompiled()
        return super.sign(convertTransactionDataAddress(uncompiledTransaction), signer)
    }

    private fun convertTransactionDataAddress(transactionData: TransactionData.Uncompiled) = transactionData.copy(
        destinationAddress = convertAddress(transactionData.destinationAddress),
    )

    private fun convertAddress(destinationAddress: String): String {
        return DecimalAddressService.convertDelAddressToDscAddress(destinationAddress)
    }
}