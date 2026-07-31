package com.tangem.blockchain.blockchains.bitcoincash.psbt

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.psbt.PsbtTransactionExtractor
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.ScriptWitness
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

/**
 * [PsbtTransactionExtractor] for Bitcoin Cash.
 *
 * acinq's [Psbt.extract] cannot be used: it validates the assembled transaction with
 * `Transaction.correctlySpends(..., STANDARD_SCRIPT_VERIFY_FLAGS)`, whose `SCRIPT_VERIFY_STRICTENC` check
 * rejects BCH's `SIGHASH_ALL | FORKID` (0x41) as an undefined Bitcoin hash type. Even without that check,
 * acinq's interpreter would recompute the legacy Bitcoin sighash while the signature commits to the
 * FORKID (BIP143-style) preimage. So a *correctly* signed BCH transaction always fails there ([REDACTED_TASK_KEY]).
 *
 * This extractor therefore performs the same assembly as [Psbt.extract] — including its structural checks —
 * but skips the Bitcoin-rules script validation. Consequently a malformed signature is only rejected by the
 * network; [com.tangem.blockchain.blockchains.bitcoincash.psbt.BitcoinCashSighashStrategy] and its tests are
 * what keep the signature honest.
 */
internal object BitcoinCashPsbtTransactionExtractor : PsbtTransactionExtractor {

    override fun extract(psbt: Psbt): Result<Transaction> {
        val unsignedTx = psbt.global.tx

        val finalTxIn = unsignedTx.txIn.mapIndexed { index, txIn ->
            val input = psbt.inputs.getOrNull(index)
                ?: return failure("Bitcoin Cash PSBT has no input #$index")

            if (input.scriptWitness != null) {
                return failure("Bitcoin Cash PSBT input #$index is a SegWit input, which Bitcoin Cash does not support")
            }
            val scriptSig = input.scriptSig
                ?: return failure("Bitcoin Cash PSBT input #$index is not finalized")

            txIn.copy(
                signatureScript = ByteVector(Script.write(scriptSig)),
                witness = ScriptWitness.empty,
            )
        }

        return Result.Success(unsignedTx.copy(txIn = finalTxIn))
    }

    private fun failure(message: String) = Result.Failure(BlockchainSdkError.CustomError(message))
}