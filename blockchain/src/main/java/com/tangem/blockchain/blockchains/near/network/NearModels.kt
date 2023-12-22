package com.tangem.blockchain.blockchains.near.network

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.near.network.Yocto.Companion.YOCTO_DECIMALS
import com.tangem.common.extensions.hexToBytes
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Basic unit of the Near blockchain corresponding to the Yocto metric prefix
 * @see <a href="https://docs.near.org/concepts/basics/accounts/state#accounts-metadata">Docs</a>
 * @author Anton Zhilenkov on 03.08.2023.
 */
class Yocto(
    val value: BigInteger,
) {

    constructor(yocto: String) : this(BigInteger(yocto))

    operator fun plus(yocto: Yocto): Yocto {
        return Yocto(value.plus(yocto.value))
    }

    operator fun minus(yocto: Yocto): Yocto {
        return Yocto(value.minus(yocto.value))
    }

    operator fun times(yocto: Yocto): Yocto {
        return Yocto(value.times(yocto.value))
    }

    fun toByteString(): ByteString {
        return ByteString.copyFrom(value.toByteArray())
    }

    internal companion object {
        const val YOCTO_DECIMALS = 24
    }
}

/**
 * Amount that simplified operations with the Near currency
 */
data class NearAmount(val yocto: Yocto) {

    val value: BigDecimal by lazy { yocto.value.toBigDecimal().movePointLeft(YOCTO_DECIMALS) }

    constructor(nearValue: BigDecimal) : this(Yocto(nearValue.movePointRight(YOCTO_DECIMALS).toBigInteger()))

    operator fun plus(near: NearAmount): NearAmount {
        return NearAmount(yocto.plus(near.yocto))
    }

    operator fun minus(near: NearAmount): NearAmount {
        return NearAmount(yocto.minus(near.yocto))
    }

    operator fun times(near: NearAmount): NearAmount {
        return NearAmount(yocto.times(near.yocto))
    }

    /**
     * @return Yocto value in hex little endian format
     */
    @Suppress("MagicNumber")
    fun toLittleEndian(): ByteArray {
        var hexString = yocto.value.toString(16)

        val leadingZeroesCount = 32 - hexString.length
        hexString = "0".repeat(leadingZeroesCount) + hexString

        return hexString.chunked(2).reversed().joinToString("").hexToBytes()
    }

    companion object {
        /**
         * Accounts must have enough tokens to cover its storage which currently costs 0.0001 NEAR per byte.
         * This equates to a minimum balance of 0.00182 Ⓝ for an account with one access key.
         * @see <a href="https://docs.near.org/integrator/accounts#introduction">Docs</a>
         */
        val DEPOSIT_VALUE = BigDecimal("0.00182")
    }
}

/**
 * The result object when requesting account information.
 */
sealed class NearAccount {
    /**
     * An object corresponding to an existing account with its amount
     */
    data class Full(
        val near: NearAmount,
        val blockHash: String,
        val storageUsage: NearAmount,
    ) : NearAccount()

    /**
     * An object corresponding to a non-existent account
     */
    object NotInitialized : NearAccount()
}

data class NearNetworkStatus(
    val chainId: String,
    val latestBlockHash: String,
    val latestBlockHeight: Long,
    val latestBlockTime: String,
    val syncing: Boolean,
)

/**
 * The result object when requesting information about an access key of the account
 * @property nextNonce unique number or nonce is required for each transaction signed with an access key. To ensure
 * a unique number is created for each transaction, the current nonce should be queried and then incremented by 1
 * @see <a href="https://docs.near.org/integrator/create-transactions#4-nonceforpublickey">Docs</a>
 */
data class AccessKey(
    val nonce: Long,
    val blockHeight: Long,
    val blockHash: String,
) {
    val nextNonce: Long = nonce + 1
}

data class NearGasPrice(
    val yoctoGasPrice: Yocto,
    val blockHash: String,
)

data class NearSentTransaction(
    val hash: String,
    val isSuccessful: Boolean,
)

class NearGetAccessKeyParams(
    val address: String,
    val publicKeyEncodedToBase58: String,
)

class NearGetTxParams(
    val txHash: String,
    val senderId: String,
)
