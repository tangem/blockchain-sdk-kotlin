package com.tangem.blockchain.blockchains.bitcoincash.psbt

import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransaction
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.common.psbt.PsbtSighashStrategy
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.psbt.Psbt
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script

/**
 * [PsbtSighashStrategy] for Bitcoin Cash: SIGHASH_ALL | SIGHASH_FORKID (0x41).
 *
 * BCH diverged from Bitcoin's sighash algorithm with the FORKID upgrade (BIP143-style preimage,
 * distinct from legacy Bitcoin's un-forked SIGHASH_ALL). This bridges the acinq [Psbt] (used for
 * cross-chain PSBT parsing/finalization) into the proven bitcoinj [BitcoinCashTransaction], which
 * already implements the FORKID sighash algorithm correctly (see [BitcoinCashTransactionBuilder]).
 *
 * [blockchain] is validated at construction time for API symmetry with other [PsbtSighashStrategy]
 * implementations and to fail fast if wired up for a non-BCH chain; the sighash preimage itself
 * (BIP143-style, FORKID) is not network-parameter dependent, so [MainNetParams] is used
 * unconditionally to satisfy bitcoinj's [org.bitcoinj.core.Transaction] constructor.
 */
internal class BitcoinCashSighashStrategy(blockchain: Blockchain) : PsbtSighashStrategy {

    init {
        require(blockchain == Blockchain.BitcoinCash || blockchain == Blockchain.BitcoinCashTestnet) {
            "BitcoinCashSighashStrategy is only valid for BitcoinCash/BitcoinCashTestnet, got $blockchain"
        }
    }

    override val sighashByte: Int = PsbtSighash.ALL_FORKID

    private val networkParameters = MainNetParams()

    override fun computeHashToSign(psbt: Psbt, inputIndex: Int, sighashType: Int): Result<ByteArray> {
        if (sighashType != sighashByte) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Bitcoin Cash only supports SIGHASH_ALL|FORKID (0x41), got 0x${sighashType.toString(HEX_RADIX)}",
                ),
            )
        }

        return try {
            val (prevScript, prevAmount) = prevOut(psbt, inputIndex)
                ?: return Result.Failure(
                    BlockchainSdkError.CustomError(
                        "BCH input $inputIndex has neither witnessUtxo nor nonWitnessUtxo",
                    ),
                )

            val tx = buildBitcoinCashTx(psbt)
            val hash = tx.hashForSignatureWitness(
                inputIndex,
                prevScript,
                Coin.valueOf(prevAmount),
                Transaction.SigHash.ALL,
                false,
            ).bytes

            Result.Success(hash)
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.CustomError("Failed to compute BCH FORKID signature hash: ${e.message}"))
        }
    }

    /**
     * Reconstructs a bitcoinj [BitcoinCashTransaction] from the acinq [psbt]'s global unsigned tx,
     * preserving version/lockTime/inputs(outpoint+sequence)/outputs so that
     * [BitcoinCashTransaction.hashForSignatureWitness] computes over an identical preimage to what
     * a fully-native BCH implementation would.
     *
     * Endianness note: acinq's [fr.acinq.bitcoin.OutPoint.hash] ([fr.acinq.bitcoin.TxHash]) is the raw
     * wire-order (internal) txid, written as-is into the serialized transaction. bitcoinj's
     * [Sha256Hash.wrap] expects bytes in *display* order (the human-readable/RPC big-endian txid) since
     * bitcoinj reverses on serialization (see `TransactionOutPoint#bitcoinSerializeToStream`). acinq's
     * [fr.acinq.bitcoin.OutPoint.txid] ([fr.acinq.bitcoin.TxId]) is exactly that reversed/display-order
     * value, so it — not `.hash` — is what must be passed to [Sha256Hash.wrap].
     */
    private fun buildBitcoinCashTx(psbt: Psbt): BitcoinCashTransaction {
        val tx = BitcoinCashTransaction(networkParameters)
        tx.setVersion(psbt.global.tx.version.toInt())

        for (txIn in psbt.global.tx.txIn) {
            val input = tx.addInput(
                Sha256Hash.wrap(txIn.outPoint.txid.value.toByteArray()),
                txIn.outPoint.index,
                Script(ByteArray(0)),
            )
            input.sequenceNumber = txIn.sequence
        }

        for (txOut in psbt.global.tx.txOut) {
            tx.addOutput(Coin.valueOf(txOut.amount.toLong()), Script(txOut.publicKeyScript.toByteArray()))
        }

        tx.lockTime = psbt.global.tx.lockTime

        return tx
    }

    /** Returns the previous output's (scriptPubKey, amountSat) for [index], preferring witnessUtxo. */
    private fun prevOut(psbt: Psbt, index: Int): Pair<ByteArray, Long>? {
        val input = psbt.inputs[index]
        input.witnessUtxo?.let { return it.publicKeyScript.toByteArray() to it.amount.toLong() }

        val nonWitnessUtxo = input.nonWitnessUtxo ?: return null
        val outputIndex = psbt.global.tx.txIn[index].outPoint.index.toInt()
        val prevTxOut = nonWitnessUtxo.txOut.getOrNull(outputIndex) ?: return null
        return prevTxOut.publicKeyScript.toByteArray() to prevTxOut.amount.toLong()
    }

    private companion object {
        const val HEX_RADIX = 16
    }
}