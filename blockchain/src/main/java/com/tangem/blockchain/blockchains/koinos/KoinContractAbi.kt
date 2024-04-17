package com.tangem.blockchain.blockchains.koinos

import com.squareup.wire.ProtoAdapter
import com.tangem.blockchain.blockchains.koinos.network.KoinContractOperation
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import koinos.contracts.token.balance_of_arguments
import koinos.contracts.token.balance_of_result
import koinos.contracts.token.transfer_arguments
import koinos.contracts.token.transfer_event
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * Helper class for encoding and decoding data related to a koin-like smart-contract in the Koinos blockchain
 *
 * **For KOIN, we assume that the coin value in Satoshi will not reach the Long.MAX_VALUE**
 *
 * **Current total supply in Satoshi: 3833938450682459**
 *
 * @see <a href="https://github.com/koinos/koinos-proto/blob/master/koinos/contracts/token/token.proto">koinos/contracts/token/token.proto</a>
 */
internal class KoinContractAbi(isTestnet: Boolean) {

    val contractId: String = if (isTestnet) CONTRACT_ID_TESTNET else CONTRACT_ID
    val chainId: String = if (isTestnet) CHAIN_ID_TESTNET else CHAIN_ID
    val balanceOf = BalanceOf()
    val transfer = Transfer()

    fun addressToByteString(address: String) = address.addressToByteString()

    class BalanceOf : KoinContractOperation {
        override val entryPoint = 0x5c721497
        override val name: String = "balance_of"
        override val argsName: String = "koinos.contracts.token.balance_of_arguments"
        override val resultName: String = "koinos.contracts.token.balance_of_result"

        fun encodeArgs(accountAddress: String): String? {
            return balance_of_arguments(
                owner = accountAddress.addressToByteString() ?: return null,
            ).encodeByteString().base64Url()
        }

        fun decodeResult(encodedString: String): Result? {
            val decoded = encodedString.decode(balance_of_result.ADAPTER) ?: return null
            return Result(balance = decoded.value_)
        }

        @JvmInline
        value class Result(val balance: Long)
    }

    class Transfer : KoinContractOperation {
        override val entryPoint = 0x27f576ca
        override val name: String = "transfer"
        override val argsName: String = "koinos.contracts.token.transfer_arguments"
        override val resultName: String = "koinos.contracts.token.transfer_result"
        override val eventName: String = "koinos.contracts.token.transfer_event"

        fun argsToProto(fromAccount: String, toAccount: String, value: Long): transfer_arguments? {
            return transfer_arguments(
                from = fromAccount.addressToByteString() ?: return null,
                to = toAccount.addressToByteString() ?: return null,
                value_ = value,
            )
        }

        fun decodeEvent(encodedString: String): Event? {
            val decoded = encodedString.decode(transfer_event.ADAPTER)
                ?: return null

            return Event(
                fromAccount = decoded.from.toByteArray().encodeBase58(),
                toAccount = decoded.to.toByteArray().encodeBase58(),
                value = decoded.value_,
            )
        }

        data class Event(
            val fromAccount: String,
            val toAccount: String,
            val value: Long,
        )
    }

    private companion object {
        const val CONTRACT_ID: String = "15DJN4a8SgrbGhhGksSBASiSYjGnMU8dGL"
        const val CONTRACT_ID_TESTNET: String = "1FaSvLjQJsCJKq5ybmGsMMQs8RQYyVv8ju"
        const val CHAIN_ID: String = "EiBZK_GGVP0H_fXVAM3j6EAuz3-B-l3ejxRSewi7qIBfSA=="
        const val CHAIN_ID_TESTNET: String = "EiBncD4pKRIQWco_WRqo5Q-xnXR7JuO3PtZv983mKdKHSQ=="

        fun String.addressToByteString(): ByteString? = decodeBase58()?.toByteString()

        fun <T> String.decode(adapter: ProtoAdapter<T>): T? {
            val bytes = decodeBase64()?.toByteArray() ?: return null
            return runCatching { adapter.decode(bytes) }.getOrNull()
        }
    }
}