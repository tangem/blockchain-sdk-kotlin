package com.tangem.blockchain.blockchains.solana.solanaj.rpc

import android.util.Base64
import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.model.*
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.MapUtils
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.Transaction
import org.p2p.solanaj.rpc.RpcApi
import org.p2p.solanaj.rpc.RpcClient
import org.p2p.solanaj.rpc.RpcException
import org.p2p.solanaj.rpc.types.RecentBlockhash
import org.p2p.solanaj.rpc.types.config.Commitment
import org.p2p.solanaj.ws.listeners.NotificationEventListener

/**
[REDACTED_AUTHOR]
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
    fun sendSignedTransaction(
        signedTransaction: ByteArray,
        maxRetries: Int = 12,
        skipPreflight: Boolean = false,
        commitment: Commitment = Commitment.FINALIZED,
    ): String {
        val base64Trx = Base64.encodeToString(signedTransaction, Base64.NO_WRAP)
        val params = buildList {
            add(base64Trx)

            val additionalParams = buildMap<String, Any> {
                this["encoding"] = "base64"
                this["maxRetries"] = maxRetries
                this["skipPreflight"] = skipPreflight
                this["commitment"] = commitment.value
            }
            add(additionalParams)
        }

        return client.call("sendTransaction", params, String::class.java)
    }

    fun getFeeForMessage(transaction: SolanaTransaction, commitment: Commitment): FeeInfo {
        return getFeeForMessage(transaction.getSerializedMessage(), commitment)
    }

    fun getFeeForMessage(transaction: ByteArray, commitment: Commitment): FeeInfo {
        val message = Base64.encodeToString(transaction, Base64.NO_WRAP)
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

    fun getTableLookupInfo(account: PublicKey, additionalParams: Map<String, Any>): NewSolanaAccountInfo {
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

    /**
     * Same as [RpcApi.getTokenAccountsByOwner] but returns improved response [NewSolanaTokenAccountInfo]
     * */
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

    /**
     * Same as [RpcApi.getSplTokenAccountInfo] but returns improved response [NewSplTokenAccountInfo]
     * */
    fun getSplTokenAccountInfoNew(account: PublicKey): NewSplTokenAccountInfo {
        val params = buildList {
            add(account.toString())

            val paramsMap = buildMap {
                this["encoding"] = "jsonParsed"
            }

            add(paramsMap)
        }

        return client.call(
            "getAccountInfo",
            params,
            NewSplTokenAccountInfo::class.java,
        ) as NewSplTokenAccountInfo
    }

    /**
     * Same as [RpcApi.getSplTokenAccountInfo] but returns response [EmptyDataSplTokenAccountInfo] without data
     * */
    fun getSplTokenAccountInfoWithEmptyData(account: PublicKey): EmptyDataSplTokenAccountInfo {
        val params = buildList {
            add(account.toString())

            val paramsMap = buildMap {
                this["encoding"] = "jsonParsed"
            }

            add(paramsMap)
        }

        return client.call(
            "getAccountInfo",
            params,
            EmptyDataSplTokenAccountInfo::class.java,
        ) as EmptyDataSplTokenAccountInfo
    }

    @Suppress("UNCHECKED_CAST")
    fun getRecentPrioritizationFees(accounts: List<PublicKey>): List<PrioritizationFee> {
        val params = buildList {
            add(accounts.map(PublicKey::toString))
        }

        val rawResult: List<Map<String, Any>> = client.call(
            "getRecentPrioritizationFees",
            params,
            List::class.java,
        ) as List<Map<String, Any>>

        // All questions to the solanaj developers...
        return rawResult.mapNotNull { item ->
            PrioritizationFee(
                slot = (item["slot"] as? Double)?.toLong() ?: return@mapNotNull null,
                prioritizationFee = (item["prioritizationFee"] as? Double)?.toLong() ?: return@mapNotNull null,
            )
        }
    }

    fun getLatestBlockhashInfo(commitment: Commitment): SolanaBlockhashInfo {
        val params = buildList {
            add(mapOf("commitment" to commitment.value))
        }

        val recentBlockhash = client.call("getLatestBlockhash", params, RecentBlockhash::class.java)
        return SolanaBlockhashInfo(
            blockhash = recentBlockhash.value.blockhash,
            slot = recentBlockhash.context.slot,
        )
    }
}