package com.tangem.blockchain.blockchains.sui.bcs

/**
 * The subset of Sui `TransactionData::V1` with a programmable transaction that the wallet builds itself.
 * Enum tags follow the variant order in `sui-types/src/transaction.rs`, which BCS encodes as the variant index.
 */
internal data class SuiTransactionData(
    val inputs: List<SuiCallArg>,
    val commands: List<SuiCommand>,
    val sender: String,
    val gasPayment: List<SuiObjectRef>,
    val gasPrice: Long,
    val gasBudget: Long,
    val expiration: SuiTransactionExpiration,
) {

    fun toBcs(): ByteArray = SuiBcsWriter()
        .u8(TRANSACTION_DATA_V1)
        .u8(TRANSACTION_KIND_PROGRAMMABLE)
        .vector(inputs) { it.write(this) }
        .vector(commands) { it.write(this) }
        .address(sender)
        .vector(gasPayment) { it.write(this) }
        .address(sender)
        .u64(gasPrice)
        .u64(gasBudget)
        .also { expiration.write(it) }
        .toByteArray()

    private companion object {
        const val TRANSACTION_DATA_V1 = 0
        const val TRANSACTION_KIND_PROGRAMMABLE = 0
    }
}

internal data class SuiObjectRef(val objectId: String, val version: Long, val digest: String) {
    fun write(writer: SuiBcsWriter) {
        writer.address(objectId).u64(version).digest(digest)
    }
}

internal sealed interface SuiCallArg {

    fun write(writer: SuiBcsWriter)

    data class PureU64(val value: Long) : SuiCallArg {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_PURE).bytes(SuiBcsWriter().u64(value).toByteArray())
        }
    }

    data class PureAddress(val address: String) : SuiCallArg {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_PURE).bytes(SuiBcsWriter().address(address).toByteArray())
        }
    }

    data class OwnedObject(val ref: SuiObjectRef) : SuiCallArg {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_OBJECT).u8(OBJECT_ARG_IMM_OR_OWNED)
            ref.write(writer)
        }
    }

    /** Reserves up to [amount] of `Balance<coinType>` from the sender's address balance (SIP-58) */
    data class BalanceWithdrawal(val amount: Long, val coinType: SuiTypeTag) : SuiCallArg {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_FUNDS_WITHDRAWAL)
                .u8(RESERVATION_MAX_AMOUNT_U64).u64(amount)
                .u8(WITHDRAWAL_TYPE_BALANCE)
            coinType.write(writer)
            writer.u8(WITHDRAW_FROM_SENDER)
        }
    }

    private companion object {
        const val TAG_PURE = 0
        const val TAG_OBJECT = 1
        const val TAG_FUNDS_WITHDRAWAL = 2
        const val OBJECT_ARG_IMM_OR_OWNED = 0
        const val RESERVATION_MAX_AMOUNT_U64 = 0
        const val WITHDRAWAL_TYPE_BALANCE = 0
        const val WITHDRAW_FROM_SENDER = 0
    }
}

internal sealed interface SuiArgument {

    fun write(writer: SuiBcsWriter)

    data object GasCoin : SuiArgument {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_GAS_COIN)
        }
    }

    data class Input(val index: Int) : SuiArgument {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_INPUT).u16(index)
        }
    }

    data class Result(val index: Int) : SuiArgument {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_RESULT).u16(index)
        }
    }

    data class NestedResult(val index: Int, val resultIndex: Int) : SuiArgument {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_NESTED_RESULT).u16(index).u16(resultIndex)
        }
    }

    private companion object {
        const val TAG_GAS_COIN = 0
        const val TAG_INPUT = 1
        const val TAG_RESULT = 2
        const val TAG_NESTED_RESULT = 3
    }
}

internal sealed interface SuiCommand {

    fun write(writer: SuiBcsWriter)

    data class MoveCall(
        val packageId: String,
        val module: String,
        val function: String,
        val typeArguments: List<SuiTypeTag>,
        val arguments: List<SuiArgument>,
    ) : SuiCommand {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_MOVE_CALL)
                .address(packageId)
                .string(module)
                .string(function)
                .vector(typeArguments) { it.write(this) }
                .vector(arguments) { it.write(this) }
        }
    }

    data class TransferObjects(val objects: List<SuiArgument>, val recipient: SuiArgument) : SuiCommand {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_TRANSFER_OBJECTS).vector(objects) { it.write(this) }
            recipient.write(writer)
        }
    }

    data class SplitCoins(val coin: SuiArgument, val amounts: List<SuiArgument>) : SuiCommand {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_SPLIT_COINS)
            coin.write(writer)
            writer.vector(amounts) { it.write(this) }
        }
    }

    data class MergeCoins(val destination: SuiArgument, val sources: List<SuiArgument>) : SuiCommand {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_MERGE_COINS)
            destination.write(writer)
            writer.vector(sources) { it.write(this) }
        }
    }

    private companion object {
        const val TAG_MOVE_CALL = 0
        const val TAG_TRANSFER_OBJECTS = 1
        const val TAG_SPLIT_COINS = 2
        const val TAG_MERGE_COINS = 3
    }
}

internal sealed interface SuiTransactionExpiration {

    fun write(writer: SuiBcsWriter)

    data object None : SuiTransactionExpiration {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_NONE)
        }
    }

    /**
     * Required when gas is paid from the address balance: there is no gas object whose version change would
     * prevent a replay, so validators accept the transaction only within [minEpoch]..[maxEpoch] (two epochs at most)
     * and dedupe it by digest, which [nonce] keeps unique.
     */
    data class ValidDuring(
        val minEpoch: Long,
        val maxEpoch: Long,
        val chainIdentifier: String,
        val nonce: Long,
    ) : SuiTransactionExpiration {
        override fun write(writer: SuiBcsWriter) {
            writer.u8(TAG_VALID_DURING)
                .optionU64(minEpoch)
                .optionU64(maxEpoch)
                .optionU64(null)
                .optionU64(null)
                .digest(chainIdentifier)
                .u32(nonce)
        }
    }

    private companion object {
        const val TAG_NONE = 0
        const val TAG_VALID_DURING = 2
    }
}