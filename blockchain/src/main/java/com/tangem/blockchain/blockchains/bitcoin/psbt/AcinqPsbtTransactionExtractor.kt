package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.psbt.PsbtTransactionExtractor
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either

/**
 * [PsbtTransactionExtractor] for chains that follow Bitcoin's script rules: Bitcoin, Litecoin, Dogecoin, Dash.
 *
 * Delegates to acinq's [Psbt.extract], which additionally validates the assembled transaction with
 * `Transaction.correctlySpends(..., STANDARD_SCRIPT_VERIFY_FLAGS)` — a real safety net for these chains,
 * catching a malformed signature locally instead of at broadcast.
 */
internal object AcinqPsbtTransactionExtractor : PsbtTransactionExtractor {

    override fun extract(psbt: Psbt): Result<Transaction> = when (val result = psbt.extract()) {
        is Either.Right -> Result.Success(result.value)
        is Either.Left -> Result.Failure(
            BlockchainSdkError.CustomError("PSBT is not finalized or cannot be extracted: ${result.value}"),
        )
    }
}