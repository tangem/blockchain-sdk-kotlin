package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import android.util.Base64
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.Transaction
import org.p2p.solanaj.rpc.RpcApi
import org.p2p.solanaj.rpc.RpcClient
import org.p2p.solanaj.rpc.RpcException
import org.p2p.solanaj.rpc.types.config.RpcSendTransactionConfig
import org.p2p.solanaj.ws.listeners.NotificationEventListener

/**
[REDACTED_AUTHOR]
 */
class RpcApi(rpcClient: RpcClient) : RpcApi(rpcClient) {

    @Deprecated("Use signAndSendTransaction instead")
    @Throws(UnsupportedOperationException::class)
    override fun sendTransaction(transaction: Transaction, signer: Account): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("Use signAndSendTransaction instead")
    @Throws(UnsupportedOperationException::class)
    override fun sendTransaction(transaction: Transaction, signer: Account, recentBlockHash: String?): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("Use signAndSendTransaction instead")
    @Throws(UnsupportedOperationException::class)
    override fun sendTransaction(
        transaction: Transaction, signers: List<Account>, recentBlockHash: String?): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("Don't use it at all, or make your own realization")
    @Throws(UnsupportedOperationException::class)
    override fun sendAndConfirmTransaction(
        transaction: Transaction, signers: List<Account>, listener: NotificationEventListener) {
        throw UnsupportedOperationException()
    }

    @Throws(RpcException::class)
    fun sendSignedTransaction(transaction: Transaction): String {
        val serializedTransaction = transaction.serialize()
        val base64Trx = Base64.encodeToString(serializedTransaction, Base64.NO_WRAP)
        val params = mutableListOf<Any>(base64Trx, RpcSendTransactionConfig())

        return client.call("sendTransaction", params, String::class.java)
    }
}