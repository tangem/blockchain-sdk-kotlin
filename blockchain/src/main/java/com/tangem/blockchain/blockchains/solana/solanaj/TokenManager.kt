package com.tangem.blockchain.blockchains.solana.solanaj

import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.rpc.RpcClient
import org.p2p.solanaj.token.TokenManager

/**
[REDACTED_AUTHOR]
 * Reimplement!, because inside, the signature of the transaction is performed, which must be taken out
 */
class TokenManager(rpcClient: RpcClient) : TokenManager(rpcClient) {

    @Deprecated("Must impl")
    override fun transfer(
        owner: Account,
        source: PublicKey,
        destination: PublicKey,
        tokenMint: PublicKey,
        amount: Long,
    ): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("")
    override fun transferCheckedToSolAddress(
        owner: Account?,
        source: PublicKey?,
        destination: PublicKey?,
        tokenMint: PublicKey?,
        amount: Long,
        decimals: Byte,
    ): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("")
    override fun initializeAccount(newAccount: Account?, usdcTokenMint: PublicKey?, owner: Account?): String {
        throw UnsupportedOperationException()
    }
}