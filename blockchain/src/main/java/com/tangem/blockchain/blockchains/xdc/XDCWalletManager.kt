package com.tangem.blockchain.blockchains.xdc

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

internal class XDCWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider) {

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return super.send(convertTransactionDataAddress(transactionData), signer)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, CompiledEthereumTransaction>> {
        return super.sign(convertTransactionDataAddress(transactionData), signer)
    }

    private fun convertTransactionDataAddress(transactionData: TransactionData) = transactionData.copy(
        sourceAddress = XDCAddressService.formatWith0xPrefix(transactionData.sourceAddress),
        destinationAddress = XDCAddressService.formatWith0xPrefix(transactionData.destinationAddress),
        contractAddress = transactionData.contractAddress?.let {
            XDCAddressService.formatWith0xPrefix(it)
        },
    )
}
