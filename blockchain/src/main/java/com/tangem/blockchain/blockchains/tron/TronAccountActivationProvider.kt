package com.tangem.blockchain.blockchains.tron

import com.tangem.blockchain.extensions.Result

/**
 * Tells whether the wallet's own Tron account has been activated on-chain. An address can hold
 * TRC-20 tokens before its account exists, but cannot broadcast a transaction until it does.
 * Implemented by [TronWalletManager]; the app reaches it by casting the wallet manager.
 */
interface TronAccountActivationProvider {
    suspend fun isAccountActivated(): Result<Boolean>
}