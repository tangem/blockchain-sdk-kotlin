package com.tangem.blockchain.blockchains.filecoin

import android.util.Log
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinAccountInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.removeLeadingZero
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Filecoin [WalletManager]
 *
 * @param wallet            wallet
 * @property networkService network service
 *
[REDACTED_AUTHOR]
 */
internal class FilecoinWalletManager(
    wallet: Wallet,
    private val networkService: FilecoinNetworkService,
) : WalletManager(wallet) {

    override val currentHost: String
        get() = networkService.baseUrl

    private var nonce: Long = 0

    override suspend fun updateInternal() {
        when (val result = networkService.getAccountInfo(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val transactionInfo = FilecoinTxInfo(
            sourceAddress = wallet.address,
            destinationAddress = destination,
            amount = amount.longValueOrZero,
            nonce = nonce,
        )

        val txGasInfo = networkService.estimateMessageGas(transactionInfo).successOr { return it }

        return Result.Success(
            data = TransactionFee.Single(
                Fee.Filecoin(
                    amount = amount.copy(
                        value = BigDecimal(txGasInfo.gasUnitPrice * txGasInfo.gasLimit)
                            .movePointLeft(wallet.blockchain.decimals()),
                    ),
                    gasUnitPrice = txGasInfo.gasUnitPrice,
                    gasLimit = txGasInfo.gasLimit,
                    gasPremium = txGasInfo.gasPremium,
                ),
            ),
        )
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return try {
            val txBuilder = FilecoinTransactionBuilder(wallet)
            val rawTx = txBuilder.buildForSign(nonce = nonce, transactionData = transactionData)

            when (val signingResult = signer.sign(rawTx, wallet.publicKey)) {
                is CompletionResult.Failure -> Result.fromTangemSdkError(signingResult.error)
                is CompletionResult.Success -> {
                    val signedTransactionBody = txBuilder.buildForSend(
                        nonce = nonce,
                        transactionData = transactionData,
                        signature = unmarshalSignature(signingResult.data, rawTx, wallet.publicKey),
                    )

                    val result = networkService.submitTransaction(signedTransactionBody = signedTransactionBody)
                    when (result) {
                        is Result.Failure -> Result.Failure(result.error)
                        is Result.Success -> {
                            val txHash = result.data
                            wallet.addOutgoingTransaction(transactionData.updateHash(hash = txHash))
                            transactionData.hash = txHash
                            Result.Success(TransactionSendResult(txHash))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(this::class.java.simpleName, e.toBlockchainSdkError().customMessage)
            throw e
        }
    }

    private fun updateWallet(accountInfo: FilecoinAccountInfo) {
        if (accountInfo.nonce != nonce) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        }

        wallet.setCoinValue(value = accountInfo.balance.movePointLeft(wallet.blockchain.decimals()))

        nonce = accountInfo.nonce
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    // TODO: [REDACTED_JIRA]
    @Suppress("MagicNumber")
    private fun unmarshalSignature(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(fromIndex = 0, toIndex = 32))
        val s = BigInteger(1, signature.copyOfRange(fromIndex = 32, toIndex = 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            hash,
            PublicKey(
                publicKey = publicKey.blockchainKey.toDecompressedPublicKey()
                    .sliceArray(1..64),
            ),
        )
        val signatureData = SignatureData(r = ecdsaSignature.r, s = ecdsaSignature.s, v = recId.toBigInteger())

        return signatureData.r.toByteArray().removeLeadingZero() +
            signatureData.s.toByteArray().removeLeadingZero() +
            signatureData.v.toByteArray()
    }
}