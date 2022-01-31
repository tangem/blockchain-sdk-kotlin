package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.rpc.types.TokenAccountInfo
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class SolanaWalletManager(
    wallet: Wallet,
    jsonRpcProvider: RpcClient
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String = jsonRpcProvider.endpoint

    private val publicKey: PublicKey = PublicKey(wallet.address)
    private val networkService = SolanaNetworkService(jsonRpcProvider)
    private val txBuilder = SolanaTransactionBuilder()

    override suspend fun update() {
        when (val result = networkService.getInfo(publicKey)) {
            is Result.Success -> {
                val accountInfo = result.data
                wallet.setCoinValue(accountInfo.balance.toSOL())
                cardTokens.forEach { cardToken ->
                    val tokenBalance = accountInfo.tokensByMint[cardToken.contractAddress]?.balance
                        ?: BigDecimal.ZERO
                    wallet.addTokenValue(tokenBalance, cardToken)
                }
            }
            is Result.Failure -> {
                wallet.removeAllTokens()
                throw Exception(result.error)
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer)
            is AmountType.Token -> sendToken(transactionData, signer)
            AmountType.Reserve -> SimpleResult.Failure(BlockchainSdkError.UnsupportedOperation())
        }
    }

    private suspend fun sendCoin(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transaction = txBuilder.buildToSign(transactionData, networkService.getRecentBlockhash())
        return when (val signResult = signer.sign(transaction.getDataForSign(), wallet.cardId, wallet.publicKey)) {
            is CompletionResult.Success -> {
                transaction.addSignedDataSignature(signResult.data)
                val result = networkService.sendTransaction(transaction)
                result.toSimpleResult()
            }
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signResult.error)
        }
    }

    private fun sendToken(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        throw BlockchainSdkError.UnsupportedOperation()
//        val associatedSourceTokenAccountAddress = associatedTokenAddress(transactionData.sourceAddress, transactionData.contractAddress!!)
//        val amount = NSDecimalNumber(decimal: transaction.amount.value * token.decimalValue).uint64Value
//        val signer = SolanaTransactionSigner(transactionSigner: signer, cardId: wallet.cardId, walletPublicKey: wallet.publicKey)
//
//        return networkService.sendSplToken(
//            amount: amount,
//            sourceTokenAddress: associatedSourceTokenAccountAddress,
//            destinationAddress: transaction.destinationAddress,
//        token: token,
//        signer: signer
//        )
//        return SimpleResult.Success
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return when (val isDestinationAccountExist = networkService.isAccountExist(PublicKey(destination))) {
            is Result.Success -> {
                val accountCreationFee = if (isDestinationAccountExist.data) {
                    BigDecimal.ZERO
                } else {
                    when (amount.type) {
                        AmountType.Coin -> networkService.mainAccountCreationFee()
                        is AmountType.Token -> {
                            when (val result = networkService.tokenAccountCreationFee()) {
                                is Result.Success -> result.data
                                is Result.Failure -> return Result.Failure(BlockchainSdkError.FailedToLoadFee)
                            }
                        }
                        AmountType.Reserve -> return Result.Failure(BlockchainSdkError.FailedToLoadFee)
                    }
                }

                when (val result = networkService.getFees()) {
                    is Result.Success -> {
                        val feePerSignature = result.data.value.feeCalculator.lamportsPerSignature.toBigDecimal()
                        val fee = feePerSignature + accountCreationFee
                        val feeAmount = Amount(fee.toSOL(), wallet.blockchain)
                        Result.Success(listOf(feeAmount))
                    }
                    is Result.Failure -> result
                }
            }
            is Result.Failure -> isDestinationAccountExist
        }
    }

    override suspend fun addToken(token: Token): Result<Amount> {
        throw BlockchainSdkError.UnsupportedOperation()

        if (!cardTokens.contains(token)) cardTokens.add(token)

        return addTokens(listOf(token))[0]
    }

    override suspend fun addTokens(tokens: List<Token>): List<Result<Amount>> {
        throw BlockchainSdkError.UnsupportedOperation()

        tokens.forEach { if (!cardTokens.contains(it)) cardTokens.add(it) }

        return when (val result = networkService.tokenAccountsInfo(publicKey)) {
            is Result.Success -> addTokenValue(tokens, result.data).map { Result.Success(it) }
            is Result.Failure -> tokens.map { Result.Failure(result.error) }
        }
    }

    private fun addTokenValue(
        tokens: List<Token>,
        loadedSPLTokens: List<TokenAccountInfo.Value>
    ): List<Amount> {
        return tokens.map { token ->
            val amountValue = loadedSPLTokens.retrieveLamportsBy(token)?.toSOL() ?: BigDecimal.ZERO
            wallet.addTokenValue(amountValue, token)
        }
    }
}

private fun <T> Result<T>.toSimpleResult(): SimpleResult {
    return when (this) {
        is Result.Success -> SimpleResult.Success
        is Result.Failure -> SimpleResult.Failure(this.error)
    }
}
