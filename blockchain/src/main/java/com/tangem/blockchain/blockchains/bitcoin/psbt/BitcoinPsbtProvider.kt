package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.BitcoinPsbtSigner
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.psbt.PsbtProvider
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Bitcoin implementation of PSBT (Partially Signed Bitcoin Transaction) provider.
 *
 * Delegates PSBT operations to BitcoinPsbtSigner while providing the PsbtProvider interface
 * for integration with WalletManager.
 *
 * @property wallet The Bitcoin wallet instance
 * @property networkProvider Network provider for broadcasting transactions
 */
internal class BitcoinPsbtProvider(
    private val wallet: Wallet,
    private val networkProvider: BitcoinNetworkProvider,
) : PsbtProvider {

    private val psbtSigner = BitcoinPsbtSigner(wallet, networkProvider)

    override suspend fun signPsbt(psbtBase64: String, signInputs: Any, signer: TransactionSigner): Result<String> {
        val inputs = when (signInputs) {
            is List<*> -> signInputs.filterIsInstance<SignInput>()
            else -> return Result.Failure(
                BlockchainSdkError.CustomError("Invalid signInputs type: expected List<SignInput>"),
            )
        }

        return psbtSigner.signPsbt(
            psbtBase64 = psbtBase64,
            signInputs = inputs,
            signer = signer,
        )
    }

    override suspend fun broadcastPsbt(psbtBase64: String): Result<String> {
        return try {
            val psbtBytes = Base64.decode(psbtBase64, Base64.NO_WRAP)
            val psbt = when (val result = Psbt.read(psbtBytes)) {
                is fr.acinq.bitcoin.utils.Either.Right -> result.value
                is fr.acinq.bitcoin.utils.Either.Left -> {
                    return Result.Failure(
                        BlockchainSdkError.CustomError("Failed to parse PSBT for broadcast: ${result.value}"),
                    )
                }
            }
            psbtSigner.broadcastPsbt(psbt)
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to broadcast PSBT: ${e.message}"),
            )
        }
    }
}