package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.BitcoinPsbtSigner
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.psbt.PsbtAddressCodec
import com.tangem.blockchain.common.psbt.PsbtOutputInfo
import com.tangem.blockchain.common.psbt.PsbtProvider
import com.tangem.blockchain.common.psbt.PsbtSighashStrategy
import com.tangem.blockchain.common.psbt.PsbtTransactionExtractor
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Bitcoin-like (UTXO) implementation of PSBT (Partially Signed Bitcoin Transaction) provider.
 *
 * Delegates PSBT operations to BitcoinPsbtSigner while providing the PsbtProvider interface
 * for integration with WalletManager. Chain-specific address decoding and sighash computation
 * are injected via [addressCodec] and [sighashStrategy], so this class works for any
 * Bitcoin-like chain (Bitcoin, Litecoin, Dogecoin, etc.).
 *
 * @property wallet The wallet instance
 * @property networkProvider Network provider for broadcasting transactions
 * @property addressCodec Decodes scriptPubKeys into the chain's native address format
 * @property sighashStrategy Chain-specific signature-hash computation
 * @property transactionExtractor Chain-specific assembly of the final transaction from a finalized PSBT
 */
internal class BitcoinPsbtProvider(
    private val wallet: Wallet,
    private val networkProvider: BitcoinNetworkProvider,
    private val addressCodec: PsbtAddressCodec,
    private val sighashStrategy: PsbtSighashStrategy,
    private val transactionExtractor: PsbtTransactionExtractor,
) : PsbtProvider {

    private val psbtSigner = BitcoinPsbtSigner(
        wallet = wallet,
        networkProvider = networkProvider,
        sighashStrategy = sighashStrategy,
        transactionExtractor = transactionExtractor,
    )

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

    override fun parsePsbtOutputs(psbtBase64: String): Result<List<PsbtOutputInfo>> {
        return try {
            val psbtBytes = Base64.decode(psbtBase64, Base64.NO_WRAP)
            val psbt = when (val result = Psbt.read(psbtBytes)) {
                is fr.acinq.bitcoin.utils.Either.Right -> result.value
                is fr.acinq.bitcoin.utils.Either.Left -> {
                    return Result.Failure(
                        BlockchainSdkError.CustomError("Failed to parse PSBT: ${result.value}"),
                    )
                }
            }
            val outputs = psbt.global.tx.txOut.map { txOut ->
                val address = addressCodec.scriptToAddress(txOut.publicKeyScript.toByteArray())
                PsbtOutputInfo(
                    address = address,
                    amountSatoshi = txOut.amount.toLong(),
                )
            }
            Result.Success(outputs)
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to parse PSBT outputs: ${e.message}"),
            )
        }
    }

    override fun deriveSignInputs(psbtBase64: String): Result<List<SignInput>> {
        return try {
            val psbtBytes = Base64.decode(psbtBase64, Base64.NO_WRAP)
            val psbt = when (val result = Psbt.read(psbtBytes)) {
                is fr.acinq.bitcoin.utils.Either.Right -> result.value
                is fr.acinq.bitcoin.utils.Either.Left -> {
                    return Result.Failure(
                        BlockchainSdkError.CustomError("Failed to parse PSBT: ${result.value}"),
                    )
                }
            }
            val walletAddresses = wallet.addresses.mapTo(mutableSetOf()) { it.value }

            val signInputs = psbt.inputs.mapIndexedNotNull { index, input ->
                val script = inputPreviousOutputScript(psbt, input, index) ?: return@mapIndexedNotNull null
                val address = addressCodec.scriptToAddress(script)
                if (address != null && address in walletAddresses) {
                    SignInput(address = address, index = index, sighashTypes = listOf(sighashStrategy.sighashByte))
                } else {
                    null
                }
            }

            if (signInputs.isEmpty()) {
                Result.Failure(
                    BlockchainSdkError.CustomError("PSBT has no inputs belonging to this wallet"),
                )
            } else {
                Result.Success(signInputs)
            }
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to derive PSBT sign inputs: ${e.message}"),
            )
        }
    }

    override fun getPsbtFee(psbtBase64: String): Result<Long> {
        return try {
            val psbtBytes = Base64.decode(psbtBase64, Base64.NO_WRAP)
            val psbt = when (val result = Psbt.read(psbtBytes)) {
                is fr.acinq.bitcoin.utils.Either.Right -> result.value
                is fr.acinq.bitcoin.utils.Either.Left -> {
                    return Result.Failure(
                        BlockchainSdkError.CustomError("Failed to parse PSBT: ${result.value}"),
                    )
                }
            }

            var inputsSatoshi = 0L
            psbt.inputs.forEachIndexed { index, input ->
                val amount = inputPreviousOutputAmount(psbt, input, index)
                    ?: return Result.Failure(
                        BlockchainSdkError.CustomError("PSBT input #$index has no UTXO to derive its amount"),
                    )
                inputsSatoshi += amount
            }
            val outputsSatoshi = psbt.global.tx.txOut.sumOf { it.amount.toLong() }

            val feeSatoshi = inputsSatoshi - outputsSatoshi
            if (feeSatoshi < 0L) {
                return Result.Failure(
                    BlockchainSdkError.CustomError("Malformed PSBT: outputs exceed inputs (negative fee)"),
                )
            }
            Result.Success(feeSatoshi)
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Failed to compute PSBT fee: ${e.message}"),
            )
        }
    }

    /**
     * Returns the scriptPubKey of the UTXO spent by [input] (from `witnessUtxo`, or `nonWitnessUtxo`
     * resolved via the input's outpoint index), or `null` if the PSBT carries no UTXO for it.
     */
    private fun inputPreviousOutputScript(psbt: Psbt, input: fr.acinq.bitcoin.psbt.Input, index: Int): ByteArray? {
        input.witnessUtxo?.let { return it.publicKeyScript.toByteArray() }
        val nonWitnessUtxo = input.nonWitnessUtxo ?: return null
        val spentTxIn = psbt.global.tx.txIn.getOrNull(index) ?: return null
        val outpointIndex = spentTxIn.outPoint.index.toInt()
        val previousOutput = nonWitnessUtxo.txOut.getOrNull(outpointIndex) ?: return null
        return previousOutput.publicKeyScript.toByteArray()
    }

    /**
     * Returns the satoshi amount of the UTXO spent by [input] (from `witnessUtxo`, or `nonWitnessUtxo`
     * resolved via the input's outpoint index), or `null` if the PSBT carries no UTXO for it.
     */
    private fun inputPreviousOutputAmount(psbt: Psbt, input: fr.acinq.bitcoin.psbt.Input, index: Int): Long? {
        input.witnessUtxo?.let { return it.amount.toLong() }
        val nonWitnessUtxo = input.nonWitnessUtxo ?: return null
        val spentTxIn = psbt.global.tx.txIn.getOrNull(index) ?: return null
        val outpointIndex = spentTxIn.outPoint.index.toInt()
        val previousOutput = nonWitnessUtxo.txOut.getOrNull(outpointIndex) ?: return null
        return previousOutput.amount.toLong()
    }
}