package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.blockchains.tezos.TezosAddressService.Companion.calculateTezosChecksum
import com.tangem.blockchain.blockchains.tezos.network.TezosOperationContent
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.commands.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.Base58
import org.spongycastle.jcajce.provider.digest.Blake2b
import java.math.BigDecimal

class TezosTransactionBuilder(
        private val walletPublicKey: ByteArray,
        private val curve: EllipticCurve
) {
    var counter: Long? = null
    val decimals = Blockchain.Tezos.decimals()

    fun buildContents(transactionData: TransactionData,
                      publicKeyRevealed: Boolean
    ): Result<List<TezosOperationContent>> {
        if (counter == null) return Result.Failure(Exception("counter is null"))
        var counter = counter!!

        val contents = arrayListOf<TezosOperationContent>()

        if (!publicKeyRevealed) {
            counter++
            val revealOp = TezosOperationContent(
                    kind = "reveal",
                    source = transactionData.sourceAddress,
                    fee = TezosConstants.REVEAL_FEE.toMutezValueString(),
                    counter = counter.toString(),
                    gas_limit = "10000",
                    storage_limit = "0",
                    public_key = walletPublicKey.encodePublicKey()
            )
            contents.add(revealOp)
        }

        counter++
        val transactionOp = TezosOperationContent(
                kind = "transaction",
                source = transactionData.sourceAddress,
                fee = TezosConstants.TRANSACTION_FEE.toMutezValueString(),
                counter = counter.toString(),
                gas_limit = "10600",
                storage_limit = "300", // set it to 0?
                destination = transactionData.destinationAddress,
                amount = transactionData.amount.bigIntegerValue().toString()
        )
        contents.add(transactionOp)

        return Result.Success(contents)
    }

    fun forgeContents(headerHash: String, operationContents: List<TezosOperationContent>): String {
        val stringBuilder = StringBuilder(320)

        val branchHex = Base58.decodeChecked(headerHash)
                .toHexString().removePrefix(TezosConstants.BRANCH_PREFIX)
        stringBuilder.append(branchHex)

        for (operation in operationContents) {

            stringBuilder.append(
                    when (operation.kind) {
                        "reveal" -> TezosConstants.REVEAL_OPERATION_KIND
                        "transaction" -> TezosConstants.TRANSACTION_OPERATION_KIND
                        else -> throw Exception("Unsupported operation kind")
                    }
            )
            stringBuilder.append(operation.source.txEncodePublicKeyHash())
            stringBuilder.append(operation.fee.txEncodeInteger())
            stringBuilder.append(operation.counter.txEncodeInteger())
            stringBuilder.append(operation.gas_limit.txEncodeInteger())
            stringBuilder.append(operation.storage_limit.txEncodeInteger())

            // reveal operation only
            operation.public_key?.let { stringBuilder.append(it.txEncodePublicKey()) }

            // transaction operation only
            operation.amount?.let { stringBuilder.append(it.txEncodeInteger()) }
            operation.destination?.let { stringBuilder.append(it.txEncodeAddress()) }
            operation.destination?.let { stringBuilder.append("00") } // parameters for transaction operation, we don't use them yet
        }

        return stringBuilder.toString()
    }

    fun buildToSign(forgedContents: String) = Blake2b.Blake2b256()
            .digest((TezosConstants.GENERIC_OPERATION_WATERMARK + forgedContents).hexToBytes())

    fun buildToSend(signature: ByteArray, forgedContents: String) = forgedContents + signature.toHexString()

    private fun ByteArray.encodePublicKey(): String {
        val prefix = TezosConstants.getPublicKeyPrefix(curve)
        val prefixedPubKey = prefix.hexToBytes() + this.toCompressedPublicKey()
        val checksum = prefixedPubKey.calculateTezosChecksum()

        return Base58.encode(prefixedPubKey + checksum)
    }

    private fun Double.toMutezValueString(): String {
        return BigDecimal.valueOf(this).movePointRight(decimals).toString()
    }

    private fun String.txEncodePublicKeyHash(): String {
        val addressHex = Base58.decodeChecked(this).toHexString()
        val prefix = addressHex.substring(0..5)
        val newPrefix = when (prefix) {
            TezosConstants.TZ1_PREFIX -> "00"
            TezosConstants.TZ2_PREFIX -> "01"
            TezosConstants.TZ3_PREFIX -> "02"
            else -> throw Exception("Invalid address format")
        }
        return newPrefix + addressHex.removePrefix(prefix)
    }

    private fun String.txEncodePublicKey(): String {
        val publicKeyHex = Base58.decodeChecked(this).toHexString()
        val prefix = publicKeyHex.substring(0..7)
        val newPrefix = when (prefix) {
            TezosConstants.EDPK_PREFIX -> "00"
            TezosConstants.SPPK_PREFIX -> "01"
            TezosConstants.P2PK_PREFIX -> "02"
            else -> throw Exception("Invalid public key format")
        }
        return newPrefix + publicKeyHex.removePrefix(prefix)
    }

    private fun String.txEncodeAddress(): String {
        val addressHex = Base58.decodeChecked(this).toHexString()
        return when (val prefix = addressHex.substring(0..5)) {
            TezosConstants.TZ1_PREFIX, TezosConstants.TZ2_PREFIX, TezosConstants.TZ3_PREFIX -> {
                "00" + this.txEncodePublicKeyHash()
            }
            TezosConstants.KT1_PREFIX -> {
                "01" + addressHex.removePrefix(prefix) + "00"
            }
            else -> throw Exception("Invalid address format")
        }
    }

    // Zarith encoding
    private fun String.txEncodeInteger(): String {
        var nn = this.toLong()
        var result = ""

        while (true) {
            if (nn < 128) {
                if (nn < 16) {
                    result += 0
                }
                result += nn.toString(16)
                break
            } else {
                var b = nn % 128
                nn -= b
                nn /= 128
                b += 128
                result += b.toString(16)
            }
        }
        return result
    }
}