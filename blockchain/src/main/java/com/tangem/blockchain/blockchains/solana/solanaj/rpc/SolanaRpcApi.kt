package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import android.util.Base64
import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.model.FeeInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.NewSolanaAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.NewSolanaTokenAccountInfo
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.MapUtils
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.Transaction
import org.p2p.solanaj.rpc.RpcApi
import org.p2p.solanaj.rpc.RpcClient
import org.p2p.solanaj.rpc.RpcException
import org.p2p.solanaj.rpc.types.config.Commitment
import org.p2p.solanaj.rpc.types.config.RpcSendTransactionConfig
import org.p2p.solanaj.ws.listeners.NotificationEventListener

/**
 * Created by Anton Zhilenkov on 26/01/2022.
 */
internal class SolanaRpcApi(rpcClient: RpcClient) : RpcApi(rpcClient) {

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
    override fun sendTransaction(transaction: Transaction, signers: List<Account>, recentBlockHash: String?): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("Don't use it at all, or make your own realization")
    @Throws(UnsupportedOperationException::class)
    override fun sendAndConfirmTransaction(
        transaction: Transaction,
        signers: List<Account>,
        listener: NotificationEventListener,
    ) {
        throw UnsupportedOperationException()
    }

    @Throws(RpcException::class)
    fun sendSignedTransaction(transaction: SolanaTransaction): String {
        val serializedTransaction = transaction.serialize()
        val base64Trx = Base64.encodeToString(serializedTransaction, Base64.NO_WRAP)
        val params = mutableListOf<Any>(base64Trx, RpcSendTransactionConfig())

        return client.call("sendTransaction", params, String::class.java)
    }

    fun getFeeForMessage(transaction: SolanaTransaction, commitment: Commitment): FeeInfo {
        val message = Base64.encodeToString(transaction.getSerializedMessage(), Base64.NO_WRAP)
        val params = buildList {
            add(message)
            add(mapOf("commitment" to commitment.value))
        }

        return client.call("getFeeForMessage", params, FeeInfo::class.java)
    }

    /**
     * Same as [RpcApi.getAccountInfo] but returns improved [NewSolanaAccountInfo]
     * instead of [org.p2p.solanaj.rpc.types.AccountInfo]
     * */
    fun getAccountInfoNew(account: PublicKey, additionalParams: Map<String, Any>): NewSolanaAccountInfo {
        val params = buildList {
            add(account.toString())

            val parameterMap = buildMap {
                this["encoding"] = MapUtils.getOrDefault(additionalParams, "encoding", "base64")
                if (additionalParams.containsKey("commitment")) {
                    val commitment = additionalParams["commitment"] as Commitment
                    this["commitment"] = commitment.value
                }

                if (additionalParams.containsKey("dataSlice")) {
                    this["dataSlice"] = additionalParams["dataSlice"]
                }
            }

            add(parameterMap)
        }

        return client.call("getAccountInfo", params, NewSolanaAccountInfo::class.java)
    }

    fun getTokenAccountsByOwnerNew(
        accountOwner: PublicKey,
        requiredParams: Map<String, Any>,
        optionalParams: Map<String, Any> = mapOf(),
    ): NewSolanaTokenAccountInfo {
        val params = buildList {
            add(accountOwner.toString())

            val requiredParamsMap = buildMap {
                if (requiredParams.containsKey("mint")) {
                    this["mint"] = requiredParams["mint"].toString()
                } else {
                    if (!requiredParams.containsKey("programId")) {
                        throw RpcException("mint or programId are mandatory parameters")
                    }

                    this["programId"] = requiredParams["programId"].toString()
                }
            }
            add(requiredParamsMap)

            val optionalParamsMap = buildMap {
                this["encoding"] = MapUtils.getOrDefault(optionalParams, "encoding", "jsonParsed")

                if (optionalParams.containsKey("commitment")) {
                    val commitment = optionalParams["commitment"] as Commitment
                    this["commitment"] = commitment.value
                }

                if (optionalParams.containsKey("dataSlice")) {
                    this["dataSlice"] = optionalParams["dataSlice"]
                }
            }
            add(optionalParamsMap)
        }

        return client.call(
            "getTokenAccountsByOwner",
            params,
            NewSolanaTokenAccountInfo::class.java,
        ) as NewSolanaTokenAccountInfo
    }
}
