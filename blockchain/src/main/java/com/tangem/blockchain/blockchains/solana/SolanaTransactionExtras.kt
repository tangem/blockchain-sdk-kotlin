package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.TransactionExtras

/**
 * Send options for a compiled Solana transaction.
 *
 * @property shouldRefreshRecentBlockhash replace the transaction's recent blockhash with one fetched immediately before
 * signing. Meant for transactions built ahead of time by a third party (StakeKit), whose blockhash would otherwise
 * expire while the user confirms and signs. Must stay off for dApp (WalletConnect) transactions: those bytes belong to
 * the dApp and are signed as received.
 */
data class SolanaTransactionExtras(
    val shouldRefreshRecentBlockhash: Boolean,
) : TransactionExtras