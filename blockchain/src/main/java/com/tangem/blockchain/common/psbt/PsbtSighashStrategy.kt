package com.tangem.blockchain.common.psbt

import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Chain-specific signature-hash computation for a PSBT input.
 * [sighashByte] is the DEFAULT flag written into the final signature (0x01 for Bitcoin-like, 0x41 for BCH FORKID),
 * used as a fallback when the caller does not request a specific per-input sighash type.
 */
internal interface PsbtSighashStrategy {
    val sighashByte: Int

    /**
     * Computes the signature hash for [inputIndex].
     *
     * @param sighashType the caller-requested per-input sighash flag (e.g. from a WalletConnect SignInput).
     * Implementations should honor [sighashType] so the preimage matches the byte appended to the DER
     * signature, unless the chain mandates a fixed sighash (e.g. Bitcoin Cash always uses
     * SIGHASH_ALL|FORKID), in which case they reject a differing [sighashType].
     */
    fun computeHashToSign(psbt: Psbt, inputIndex: Int, sighashType: Int): Result<ByteArray>
}