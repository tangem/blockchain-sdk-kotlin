package com.tangem.blockchain.common.psbt

import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Chain-specific PSBT extractor role (BIP-174): assembles the final, ready-to-broadcast [Transaction]
 * from a fully finalized [Psbt].
 *
 * Exists as a chain-specific strategy because acinq's [Psbt.extract] validates the assembled transaction
 * against Bitcoin consensus rules, which a Bitcoin Cash transaction cannot satisfy — see
 * `BitcoinCashPsbtTransactionExtractor`.
 */
internal interface PsbtTransactionExtractor {
    fun extract(psbt: Psbt): Result<Transaction>
}