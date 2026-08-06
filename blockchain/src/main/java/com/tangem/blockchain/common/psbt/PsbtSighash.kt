package com.tangem.blockchain.common.psbt

/**
 * SIGHASH type flags used across the PSBT signing pipeline, in one place so the value a hash is
 * computed with stays in sync with the byte appended to the DER signature (and asserted in tests).
 *
 * [ALL] is the universal Bitcoin SIGHASH_ALL — the same value acinq exposes as
 * `fr.acinq.bitcoin.SigHash.SIGHASH_ALL` and bitcoinj as `Transaction.SigHash.ALL`. It is kept here
 * rather than referenced from either library so the whole feature has one library-agnostic source.
 * [FORKID] / [ALL_FORKID] are Bitcoin Cash-specific and are NOT provided by acinq or bitcoinj (both
 * are Bitcoin-only); the non-PSBT BCH transaction builder defines its own equivalent
 * (`BitcoinCashTransactionBuilder.BCH_SIGHASH_ALL_FORKID`).
 */
internal object PsbtSighash {
    /** SIGHASH_ALL — universal Bitcoin flag; matches acinq `SigHash.SIGHASH_ALL`. */
    const val ALL = 0x01

    /** SIGHASH_FORKID replay-protection bit (Bitcoin Cash); not defined by acinq/bitcoinj. */
    const val FORKID = 0x40

    /** SIGHASH_ALL | SIGHASH_FORKID (0x41) — Bitcoin Cash. */
    const val ALL_FORKID = 0x41
}