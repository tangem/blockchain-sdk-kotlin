package com.tangem.blockchain.blockchains.icp.network

import org.ic4j.candid.annotations.Field
import org.ic4j.candid.annotations.Name
import org.ic4j.candid.types.Type

class ICPBalanceRequest(
    @Name("account")
    @Field(Type.NAT8)
    val account: ByteArray,
)

class ICPTransferRequest(
    @Name("to")
    @Field(Type.NAT8)
    val to: ByteArray,

    @Name("amount")
    @Field(Type.RECORD)
    val amount: ICPAmount,

    @Name("fee")
    @Field(Type.RECORD)
    val fee: ICPAmount,

    @Name("memo")
    @Field(Type.NAT64)
    val memo: Long,

    @Name("created_at_time")
    @Field(Type.RECORD)
    val createdAtTime: ICPTimestamp,
)

class ICPAmount() {

    @Name("e8s")
    @Field(Type.NAT64)
    var value: Long? = null

    constructor(value: Long) : this() {
        this.value = value
    }
}

class ICPTimestamp() {

    @Name("timestamp_nanos")
    @Field(Type.NAT64)
    var timestampNanos: Long? = null

    constructor(timestampNanos: Long) : this() {
        this.timestampNanos = timestampNanos
    }
}

enum class ICPTransferResponse {
    Ok, Err;

    @Name("Ok")
    @Field(Type.NAT64)
    var blockIndex: Long? = null

    @Name("Err")
    @Field(Type.VARIANT)
    var errValue: ICPTransferError? = null
}

enum class ICPTransferError {
    BadFee, InsufficientFunds, TxTooOld, TxCreatedInFuture, TxDuplicate;

    @Name("BadFee")
    @Field(Type.RECORD)
    var badFeeError: BadFeeError? = null

    @Name("InsufficientFunds")
    @Field(Type.RECORD)
    var insufficientFundsError: InsufficientFundsError? = null

    @Name("TxTooOld")
    @Field(Type.RECORD)
    var txTooOldError: TxTooOldError? = null

    @Name("TxDuplicate")
    @Field(Type.RECORD)
    var txDuplicateError: TxDuplicateError? = null
}

class BadFeeError {
    @Name("expected_fee")
    @Field(Type.RECORD)
    var expectedFee: ICPAmount? = null
}

class InsufficientFundsError {
    @Name("balance")
    @Field(Type.RECORD)
    var balance: ICPAmount? = null
}

class TxTooOldError {
    @Name("allowed_window_nanos")
    @Field(Type.NAT64)
    var allowedWindowNanos: Long? = null
}

class TxDuplicateError {
    @Name("duplicate_of")
    @Field(Type.NAT64)
    var blockIndex: Long? = null
}