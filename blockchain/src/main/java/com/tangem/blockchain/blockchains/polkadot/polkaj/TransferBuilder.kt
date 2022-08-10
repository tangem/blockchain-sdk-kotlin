package com.tangem.blockchain.blockchains.polkadot.polkaj

import io.emeraldpay.polkaj.scaletypes.Extrinsic
import io.emeraldpay.polkaj.tx.AccountRequests.TransferBuilder
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Hash512

/**
[REDACTED_AUTHOR]
 * It replaced standard method TransferBuilder.sign with ExtrinsicSigner
 */
fun TransferBuilder.setSignedSignature(context: ExtrinsicContext, signedSignature: ByteArray): TransferBuilder {
    val hash512 = Hash512(signedSignature)
    val signature = Extrinsic.ED25519Signature(hash512)
    return this.nonce(context).signed(signature)
}