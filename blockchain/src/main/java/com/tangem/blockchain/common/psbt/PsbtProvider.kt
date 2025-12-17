package com.tangem.blockchain.common.psbt

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result

/**
 * Provider for PSBT (Partially Signed Bitcoin Transaction) operations.
 * Primarily used for Bitcoin and Bitcoin-like blockchains.
 */
interface PsbtProvider {
    /**
     * Signs a PSBT transaction.
     *
     * @param psbtBase64 PSBT in Base64 encoding
     * @param signInputs List of inputs to sign (blockchain-specific format)
     * @param signer Transaction signer (typically Tangem card)
     * @return Success with signed PSBT in Base64, or Failure with error
     */
    suspend fun signPsbt(psbtBase64: String, signInputs: Any, signer: TransactionSigner): Result<String>

    /**
     * Broadcasts a finalized PSBT transaction.
     *
     * @param psbtBase64 Finalized PSBT in Base64 encoding
     * @return Success with transaction hash, or Failure with error
     */
    suspend fun broadcastPsbt(psbtBase64: String): Result<String>
}