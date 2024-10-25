package com.tangem.blockchain.blockchains.casper

import com.tangem.blockchain.blockchains.casper.network.request.CasperTransactionBody
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.bouncycastle.jcajce.provider.digest.Blake2b
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigInteger

/**
 * Casper transaction builder
 *
 * @property wallet wallet
 *
 */
@Suppress("UnusedPrivateMember", "MagicNumber")
internal class CasperTransactionBuilder(private val wallet: Wallet) {

    fun buildForSign(transactionData: TransactionData): CasperTransactionBody = buildTransaction(transactionData)

    fun buildForSend(unsignedTransactionBody: CasperTransactionBody, signature: ByteArray) =
        unsignedTransactionBody.copy(
            approvals = listOf(
                CasperTransactionBody.Approval(
                    signer = unsignedTransactionBody.header.account,
                    signature = signature.toHexString().lowercase(),
                ),
            ),
        )

    @Suppress("LongMethod")
    private fun buildTransaction(transactionData: TransactionData): CasperTransactionBody {
        transactionData.requireUncompiled()

        val amount = transactionData.amount.longValueOrZero.toBigInteger()
        val fee = (transactionData.fee as Fee.Common).amount.longValueOrZero.toBigInteger()
        val id = (transactionData.extras as? CasperTransactionExtras)?.memo

        val session = CasperTransactionBody.Session(
            transfer = CasperTransactionBody.Session.Transfer(
                args = listOf(
                    listOf(
                        "amount",
                        buildCLValue(
                            type = CasperTransactionBody.CLType.U512,
                            parsed = amount,
                        ),
                    ),
                    listOf(
                        "target",
                        buildCLValue(
                            type = CasperTransactionBody.CLType.PublicKey,
                            parsed = transactionData.destinationAddress.lowercase(),
                        ),
                    ),
                    listOf(
                        "id",
                        buildCLValue(
                            type = CasperTransactionBody.CLType.Option(
                                innerType = CasperTransactionBody.CLType.U64,
                            ),
                            parsed = id,
                        ),
                    ),
                ),
            ),
        )

        val payment = CasperTransactionBody.Payment(
            moduleBytesObj = CasperTransactionBody.Payment.ModuleBytes(
                moduleBytes = "",
                args = listOf(
                    listOf(
                        "amount",
                        buildCLValue(
                            type = CasperTransactionBody.CLType.U512,
                            parsed = fee,
                        ),
                    ),
                ),
            ),
        )

        val header = CasperTransactionBody.Header(
            account = transactionData.sourceAddress.lowercase(),
            timestamp = DateTime.now().withZone(DateTimeZone.UTC).toString(),
            ttl = DEFAULT_TTL_FORMATTED,
            gasPrice = DEFAULT_GAS_PRICE,
            bodyHash = getBodyHash(payment, session),
            chainName = "casper",
        )

        val body = CasperTransactionBody(
            hash = getTransactionHash(header),
            header = header,
            payment = payment,
            session = session,
            approvals = listOf(),
        )

        return body
    }

    private fun getTransactionHash(header: CasperTransactionBody.Header): String {
        return Blake2b.Blake2b256()
            .digest(header.serialize().hexToBytes())
            .toHexString()
            .lowercase()
    }

    private fun getBodyHash(payment: CasperTransactionBody.Payment, session: CasperTransactionBody.Session): String {
        val resultSerialization = payment.moduleBytesObj.serialize() + session.transfer.serialize()
        return Blake2b.Blake2b256()
            .digest(resultSerialization.hexToBytes())
            .toHexString()
            .lowercase()
    }

    private fun buildCLValue(type: CasperTransactionBody.CLType, parsed: Any?) = CasperTransactionBody.CLValue(
        bytes = type.serializeParsed(parsed),
        clType = type,
        parsed = parsed,
    )

    private fun CasperTransactionBody.Header.serialize(): String {
        return account +
            DateTime.parse(timestamp).millis.serialize() +
            DEFAULT_TTL_MILLIS.serialize() +
            DEFAULT_GAS_PRICE.serialize() +
            bodyHash +
            serializeDependencies(dependencies) +
            chainName.serialize()
    }

    private fun CasperTransactionBody.Payment.ModuleBytes.serialize(): String {
        return "00" + moduleBytes.serialize() + serializeArgs(this.args)
    }

    private fun CasperTransactionBody.Session.Transfer.serialize(): String {
        return "05" + serializeArgs(this.args)
    }

    private fun serializeDependencies(dependencies: List<String>): String {
        return dependencies.size.serialize() + dependencies.joinToString("")
    }

    private fun serializeArgs(args: List<Any>): String {
        val count = args.size
        var result = count.serialize()
        args.forEach { arg ->
            val typedArg = arg as List<*>
            val name = typedArg[0] as String
            val value = typedArg[1] as CasperTransactionBody.CLValue
            result += name.serialize() + value.serialize()
        }
        return result
    }

    private fun CasperTransactionBody.CLValue.serialize(): String {
        val parsedSerialization = clType.serializeParsed(parsed)
        val parsedLengthSerialization = (parsedSerialization.length / 2).serialize()
        val clTypeSerialization = clType.serialize()
        return parsedLengthSerialization + parsedSerialization + clTypeSerialization
    }

    /**
     * See https://docs.casper.network/concepts/serialization/primitives/
     */
    private fun CasperTransactionBody.CLType.serializeParsed(parsed: Any?): String = when (this) {
        is CasperTransactionBody.CLType.U64 -> (parsed as Long).serialize()
        is CasperTransactionBody.CLType.U512 -> (parsed as BigInteger).serialize()
        is CasperTransactionBody.CLType.String -> (parsed as String).serialize()
        is CasperTransactionBody.CLType.PublicKey -> parsed as String
        is CasperTransactionBody.CLType.Option -> if (parsed == null) {
            "00"
        } else {
            "01" + innerType.serializeParsed(parsed)
        }
        is CasperTransactionBody.CLType.Unknown -> error("`Unknown` is invalid value for CLType")
    }

    /**
     * See https://docs.casper.network/concepts/serialization/primitives/#clvalue-cltype
     */
    private fun CasperTransactionBody.CLType.serialize(): String = when (this) {
        is CasperTransactionBody.CLType.U64 -> "05"
        is CasperTransactionBody.CLType.U512 -> "08"
        is CasperTransactionBody.CLType.String -> "0a"
        is CasperTransactionBody.CLType.PublicKey -> "16"
        is CasperTransactionBody.CLType.Option -> "0d" + this.innerType.serialize()
        is CasperTransactionBody.CLType.Unknown -> error("`Unknown` is invalid value for CLType")
    }

    private fun String.serialize(): String {
        return this.length.serialize() + this.toByteArray().toHexString().lowercase()
    }

    private fun Byte.serialize(): String = this
        .toString(16)
        .padStart(2, '0')
        .reverseHexed()

    private fun Int.serialize(): String = this
        .toString(16)
        .padStart(8, '0')
        .reverseHexed()

    private fun Long.serialize(): String = this
        .toString(16)
        .padStart(16, '0')
        .reverseHexed()

    private fun BigInteger.serialize(): String {
        if (this.equals(0)) {
            return "00"
        }
        var retStr: String = this.toString(16)
        if (retStr.length % 2 == 1) {
            retStr = "0$retStr"
        }
        val prefixLengthString: String = (retStr.length / 2).toByte().serialize()
        return prefixLengthString + retStr.reverseHexed()
    }

    private fun String.reverseHexed(): String = this
        .chunked(2)
        .reversed()
        .toList()
        .joinToString("")

    companion object {
        const val DEFAULT_TTL_FORMATTED = "30m"
        const val DEFAULT_TTL_MILLIS = 1800000L
        const val DEFAULT_GAS_PRICE = 1L
    }
}