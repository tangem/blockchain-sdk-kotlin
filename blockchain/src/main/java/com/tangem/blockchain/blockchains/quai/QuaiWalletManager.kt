package com.tangem.blockchain.blockchains.quai

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionValidator
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Result.Success
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.nft.DefaultNFTProvider
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString

class QuaiWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
    nftProvider: NFTProvider = DefaultNFTProvider,
    supportsENS: Boolean,
    yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
) : EthereumWalletManager(
    wallet = wallet,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    transactionHistoryProvider = transactionHistoryProvider,
    nftProvider = nftProvider,
    supportsENS = supportsENS,
    yieldSupplyProvider = yieldSupplyProvider,
    ethereumTransactionValidator = EthereumTransactionValidator(blockchain = wallet.blockchain),
) {

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, EthereumCompiledTxInfo>> {
        val transactionToSign = transactionBuilder.buildForSign(transaction = transactionData)
        val extendedPublicKey = wallet.publicKey.derivationType?.hdKey?.extendedPublicKey
        val path = wallet.updatedDerivationPath
        if (extendedPublicKey == null || path == null) {
            Logger.logTransaction("QuaiWalletManager $extendedPublicKey || $path, can't proceed transaction")
            return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        }
        val updatedPublicKey = Wallet.PublicKey(
            seedKey = wallet.publicKey.seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(
                    extendedPublicKey = extendedPublicKey,
                    path = path,
                ),
            ),
        )
        return when (val signResponse = signer.sign(transactionToSign.hash, updatedPublicKey)) {
            is CompletionResult.Success -> Result.Success(signResponse.data to transactionToSign)
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResponse.error)
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        validate(transactionData).onFailure {
            Result.Failure(it as? BlockchainSdkError ?: BlockchainSdkError.FailedToBuildTx)
        }

        val transactionToSend = prepareForSend(transactionData, signer)
            .successOr { return it }

        return when (val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString().formatHex())) {
            is Result.Failure -> Result.fromTangemSdkError(sendResult.error)
            is Result.Success<String> -> {
                val txHash = sendResult.data
                wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                Success(TransactionSendResult(txHash))
            }
        }
    }
}