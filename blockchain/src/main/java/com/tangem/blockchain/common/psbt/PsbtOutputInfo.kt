package com.tangem.blockchain.common.psbt

/**
 * A single output of a PSBT (Partially Signed Bitcoin Transaction).
 *
 * Used to display the recipient and amount to the user before signing, so that they don't sign blindly.
 *
 * @property address recipient address decoded from the output script, or `null` if it could not be decoded
 *                   (e.g. `OP_RETURN` or non-standard scripts)
 * @property amountSatoshi output amount, in satoshi
 */
data class PsbtOutputInfo(
    val address: String?,
    val amountSatoshi: Long,
)