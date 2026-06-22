package com.tangem.blockchain.common.psbt

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
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

    /**
     * Parses a PSBT and returns its outputs (recipient address + amount) for display before signing.
     *
     * This lets the UI show the user where funds are going and how much, instead of signing blindly.
     *
     * @param psbtBase64 PSBT in Base64 encoding
     * @return Success with the list of outputs, or Failure with error
     */
    fun parsePsbtOutputs(psbtBase64: String): Result<List<PsbtOutputInfo>>

    /**
     * Computes the on-chain miner fee carried by a PSBT, in satoshi.
     *
     * The fee is not transmitted separately by swap providers — it is implied by the transaction as
     * `sum(input amounts) - sum(output amounts)`. Input amounts are taken from each input's
     * `witnessUtxo`/`nonWitnessUtxo`; if any input has no UTXO data the fee cannot be derived and a
     * [Result.Failure] is returned.
     *
     * @param psbtBase64 PSBT in Base64 encoding
     * @return Success with the fee in satoshi, or Failure if parsing fails, a UTXO is missing, or the
     * fee is negative (malformed PSBT)
     */
    fun getPsbtFee(psbtBase64: String): Result<Long>

    /**
     * Derives the list of PSBT inputs that belong to this wallet and therefore must be signed.
     *
     * Unlike WalletConnect (where the dApp explicitly supplies the inputs to sign), swap providers
     * return a "naked" PSBT in `txData`, so the wallet has to figure out which inputs are its own.
     * For every input whose previous-output address (from `witnessUtxo`/`nonWitnessUtxo`) matches one
     * of the wallet's addresses, a [SignInput] with the default `SIGHASH_ALL` is produced. The result
     * can be passed directly to [signPsbt].
     *
     * @param psbtBase64 PSBT in Base64 encoding
     * @return Success with the inputs to sign, or Failure if parsing fails or no input belongs to the wallet
     */
    fun deriveSignInputs(psbtBase64: String): Result<List<SignInput>>
}