package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.PsbtHashComputer
import com.tangem.blockchain.common.psbt.PsbtSighash
import com.tangem.blockchain.common.psbt.PsbtSighashStrategy
import com.tangem.blockchain.extensions.Result
import fr.acinq.bitcoin.psbt.Psbt

/**
 * Default [PsbtSighashStrategy] for Bitcoin-like chains (Bitcoin, Litecoin, Dogecoin, Dash).
 *
 * Delegates to [PsbtHashComputer], honoring the caller-requested [sighashType] (so a WalletConnect
 * dApp may request a non-default per-input sighash). [sighashByte] = SIGHASH_ALL (0x01) is only the
 * fallback used when no per-input type is supplied. No FORKID.
 */
internal object DefaultSighashStrategy : PsbtSighashStrategy {

    override val sighashByte: Int = PsbtSighash.ALL

    override fun computeHashToSign(psbt: Psbt, inputIndex: Int, sighashType: Int): Result<ByteArray> {
        return PsbtHashComputer.computeHashToSign(psbt, inputIndex, sighashType)
    }
}