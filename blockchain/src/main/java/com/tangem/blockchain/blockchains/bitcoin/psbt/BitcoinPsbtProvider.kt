package com.tangem.blockchain.blockchains.bitcoin.psbt

import android.util.Base64
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.BitcoinPsbtSigner
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.psbt.PsbtOutputInfo
import com.tangem.blockchain.common.psbt.PsbtProvider
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
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

    override fun parsePsbtOutputs(psbtBase64: String): Result<List<PsbtOutputInfo>> {
        if (wallet.blockchain != Blockchain.Bitcoin && wallet.blockchain != Blockchain.BitcoinTestnet) {
            return Result.Failure(
                BlockchainSdkError.CustomError("PSBT outputs parsing is not supported for ${wallet.blockchain}"),
            )
        }
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
            val chainHash = if (wallet.blockchain.isTestnet()) {
                Block.Testnet3GenesisBlock.hash
            } else {
                Block.LivenetGenesisBlock.hash
            }
            val outputs = psbt.global.tx.txOut.map { txOut ->
                val address = when (
                    val decoded = Bitcoin.addressFromPublicKeyScript(chainHash, txOut.publicKeyScript.toByteArray())
                ) {
                    is fr.acinq.bitcoin.utils.Either.Right -> decoded.value
                    is fr.acinq.bitcoin.utils.Either.Left -> null
                }
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
        if (wallet.blockchain != Blockchain.Bitcoin && wallet.blockchain != Blockchain.BitcoinTestnet) {
            return Result.Failure(
                BlockchainSdkError.CustomError("PSBT sign-input derivation is not supported for ${wallet.blockchain}"),
            )
        }
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
            val chainHash = if (wallet.blockchain.isTestnet()) {
                Block.Testnet3GenesisBlock.hash
            } else {
                Block.LivenetGenesisBlock.hash
            }
            val walletAddresses = wallet.addresses.mapTo(mutableSetOf()) { it.value }

            val signInputs = psbt.inputs.mapIndexedNotNull { index, input ->
                val script = inputPreviousOutputScript(psbt, input, index) ?: return@mapIndexedNotNull null
                val address = when (val decoded = Bitcoin.addressFromPublicKeyScript(chainHash, script)) {
                    is fr.acinq.bitcoin.utils.Either.Right -> decoded.value
                    is fr.acinq.bitcoin.utils.Either.Left -> null
                }
                if (address != null && address in walletAddresses) {
                    SignInput(address = address, index = index, sighashTypes = listOf(SIGHASH_ALL))
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
        if (wallet.blockchain != Blockchain.Bitcoin && wallet.blockchain != Blockchain.BitcoinTestnet) {
            return Result.Failure(
                BlockchainSdkError.CustomError("PSBT fee computation is not supported for ${wallet.blockchain}"),
            )
        }
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

    private companion object {
        const val SIGHASH_ALL = 1
    }
}