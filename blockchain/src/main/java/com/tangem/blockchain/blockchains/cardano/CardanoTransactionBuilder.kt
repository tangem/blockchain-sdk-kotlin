package com.tangem.blockchain.blockchains.cardano

import android.util.Log
import co.nstant.`in`.cbor.*
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.model.Map
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.DataItem
import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.blockchains.cardano.utils.matchesCardanoAsset
import com.tangem.blockchain.blockchains.cardano.walletcore.CardanoTWTxBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.extensions.toHexString
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.ton.tl.ByteString.Companion.decodeFromHex
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Cardano
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import kotlin.properties.Delegates

// You can decode your CBOR transaction here: https://cbor.me
internal class CardanoTransactionBuilder(
    private val wallet: Wallet,
) : TransactionValidator {

    private val coinType: CoinType = wallet.blockchain.trustWalletCoinType
    private val decimals: Int = wallet.blockchain.decimals()

    private var twTxBuilder: CardanoTWTxBuilder by Delegates.notNull()

    fun update(outputs: List<CardanoUnspentOutput>) {
        twTxBuilder = CardanoTWTxBuilder(wallet = wallet, outputs = outputs)
    }

    override suspend fun validate(transactionData: TransactionData): Result<Unit> {
        return runCatching {
            transactionData.requireUncompiled()

            val isCoinTransaction = transactionData.amount.type is AmountType.Coin
            val transactionValue = transactionData.amount.value ?: BigDecimal.ZERO

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientSendingAdaAmount,
                condition = isCoinTransaction && transactionValue < BigDecimal.ONE,
            )

            val plan = AnySigner.plan(
                twTxBuilder.build(transactionData),
                coinType,
                Cardano.TransactionPlan.parser(),
            )

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientMinAdaBalanceToSendToken,
                condition = !isCoinTransaction && plan.error == Common.SigningError.Error_low_balance,
            )

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens,
                condition = checkRequiredMinAdaValue(transactionData = transactionData, plan = plan),
            )

            checkRemainingAdaBalance(transactionData = transactionData, plan = plan)
        }
    }

    fun estimateFee(transactionData: TransactionData): Fee {
        transactionData.requireUncompiled()

        // Create input with zero fee amount
        val input = twTxBuilder.build(transactionData)
        val plan = AnySigner.plan(input, coinType, Cardano.TransactionPlan.parser())

        val feeAmount = Amount(
            value = BigDecimal(plan.fee).movePointLeft(decimals),
            blockchain = wallet.blockchain,
        )

        return when (val type = transactionData.amount.type) {
            AmountType.Coin -> Fee.Common(amount = feeAmount)
            is AmountType.Token -> {
                val transactionWithNotZeroFee = transactionData.copy(fee = plan.createTokenFee())

                estimateTokenFee(transactionData = transactionWithNotZeroFee)
            }
            else -> throw BlockchainSdkError.CustomError("AmountType $type is not supported")
        }
    }

    /**
     * Estimate min-ada-value for sending a token taking into account the already calculated fee.
     * It's necessary to be sure that the remaining balance is correct.
     *
     * @param transactionData transaction with non zero fee amount
     *
     * @see CardanoTWTxBuilder.setTokenAmount
     */
    private fun estimateTokenFee(transactionData: TransactionData): Fee.CardanoToken {
        val input = twTxBuilder.build(transactionData)
        val plan = AnySigner.plan(input, coinType, Cardano.TransactionPlan.parser())

        return plan.createTokenFee()
    }

    private fun Cardano.TransactionPlan.createTokenFee(): Fee.CardanoToken {
        return Fee.CardanoToken(
            amount = Amount(
                value = BigDecimal(fee).movePointLeft(decimals),
                blockchain = wallet.blockchain,
            ),
            minAdaValue = BigDecimal(amount).movePointLeft(decimals),
        )
    }

    fun buildForSign(transactionData: TransactionData): ByteArray {
        return when (transactionData) {
            is TransactionData.Uncompiled -> {
                val input = twTxBuilder.build(transactionData) // returns Cardano.SigningInput (protobuf)
                val txInputData = input.toByteArray()          // protobuf serialization
                val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
                val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

                if (preSigningOutput.error != Common.SigningError.OK) {
                    throw BlockchainSdkError.FailedToBuildTx
                }

                preSigningOutput.dataHash.toByteArray()
            }

            is TransactionData.Compiled -> {
                val compiledBytes = (transactionData.value as TransactionData.Compiled.Data.RawString)
                    .data.decodeFromHex()
                    .toByteArray()

                val txBodyBytes = extractTxBodyFromCompiled(compiledBytes)

                val txBodyItem = CborDecoder(ByteArrayInputStream(txBodyBytes)).decode().first()
                removeTag258Recursive(txBodyItem)

                val cleanBodyBytes = ByteArrayOutputStream().use { out ->
                    CborEncoder(out).encode(listOf(txBodyItem))
                    out.toByteArray()
                }

                hashBlake2b256(cleanBodyBytes)

            }
        }
    }

    fun extractTxBodyFromCompiled(compiledTx: ByteArray): ByteArray {
        val decodedItems = CborDecoder(ByteArrayInputStream(compiledTx)).decode()
        val rootArray = decodedItems.firstOrNull() as? co.nstant.`in`.cbor.model.Array
            ?: error("Expected root CBOR array [txBody, witnessSet, metadata]")

        val txBodyItem = rootArray.dataItems.getOrNull(0) as? co.nstant.`in`.cbor.model.Map
            ?: error("TxBody (index 0) must be CBOR Map")

        val parsed = ByteArrayOutputStream().use { out ->
            CborEncoder(out).encode(listOf(txBodyItem))
            out.toByteArray()
        }

        Log.e("CardanoTx", "parsed initial: " + parsed.toHexString())

        return ByteArrayOutputStream().use { out ->
            CborEncoder(out).encode(listOf(txBodyItem))
            out.toByteArray()
        }
    }

    fun hashBlake2b256(data: ByteArray): ByteArray {
        val digest = Blake2bDigest(256)
        digest.update(data, 0, data.size)
        return ByteArray(32).also { digest.doFinal(it, 0) }
    }

    fun buildForSend(transactionData: TransactionData, signatureInfo: SignatureInfo): ByteArray {
        return buildForSend(transactionData, listOf(signatureInfo))
    }

    fun buildForSend(transactionData: TransactionData, signaturesInfo: List<SignatureInfo>): ByteArray {
        transactionData.requireCompiled()

        return buildCompiledForSend(
            (transactionData.value as TransactionData.Compiled.Data.RawString).data.decodeFromHex().toByteArray(),
            signaturesInfo
        )
        // val input = twTxBuilder.build(transactionData)
        // val txInputData = input.toByteArray()
        //
        // val signatures = DataVector()
        // val publicKeys = DataVector()
        //
        // signaturesInfo.forEach { signatureInfo ->
        //     signatures.add(signatureInfo.signature)
        //
        //     // WalletCore used here `.ed25519Cardano` curve with 128 bytes publicKey.
        //     // Calculated as: chainCode + secondPubKey + chainCode
        //     // The number of bytes in a Cardano public key (two ed25519 public key + chain code).
        //     // We should add dummy chain code in publicKey if we use old 32 byte key to get 128 bytes in total
        //     val publicKey = if (CardanoUtils.isExtendedPublicKey(signatureInfo.publicKey)) {
        //         signatureInfo.publicKey
        //     } else {
        //         signatureInfo.publicKey + ByteArray(MISSING_LENGTH_TO_EXTENDED_KEY)
        //     }
        //
        //     publicKeys.add(publicKey)
        // }
        //
        // val compileWithSignatures = TransactionCompiler.compileWithMultipleSignatures(
        //     coinType,
        //     txInputData,
        //     signatures,
        //     publicKeys,
        // )
        //
        // val output = Cardano.SigningOutput.parseFrom(compileWithSignatures)
        //
        // if (output.error != Common.SigningError.OK || output.encoded.isEmpty) {
        //     throw BlockchainSdkError.FailedToBuildTx
        // }
        //
        // return output.encoded.toByteArray()
    }

    fun buildCompiledForSend(
        compiledTx: ByteArray,
        signaturesInfo: List<SignatureInfo>,
    ): ByteArray {
        Log.e("CardanoTx", "Starting buildCompiledForSend...")

        val decoded = CborDecoder(ByteArrayInputStream(compiledTx)).decode()
        val rootArray = decoded.firstOrNull() as? Array
            ?: error("Expected root CBOR array")
        Log.e("CardanoTx", "Decoded root CBOR array")

        val txBody = rootArray.dataItems.getOrNull(0)
            ?: error("TxBody missing at index 0")
        Log.e("CardanoTx", "Extracted txBody from compiled TX")

        removeTag258Recursive(txBody)
        Log.e("CardanoTx", "Removed tag(258) from txBody")

        val witnessesArray = Array()
        signaturesInfo.forEachIndexed { index, sigInfo ->
            val vkey = sigInfo.publicKey.take(32).toByteArray() // truncate extended pubkey
            val sig = sigInfo.signature

            val witness = Array().apply {
                add(co.nstant.`in`.cbor.model.ByteString(vkey))
                add(co.nstant.`in`.cbor.model.ByteString(sig))
            }

            Log.e(
                "CardanoTx",
                "Added witness #$index:\n  pubKey=${vkey.toHexString()}\n  signature=${sig.toHexString()}"
            )

            witnessesArray.add(witness)
        }

        val witnessesMap = Map().apply {
            put(co.nstant.`in`.cbor.model.UnsignedInteger(0), witnessesArray)
        }

        val finalArray = Array().apply {
            add(txBody)
            add(witnessesMap)
            add(co.nstant.`in`.cbor.model.SimpleValue.TRUE)
            add(co.nstant.`in`.cbor.model.SimpleValue.NULL)
        }

        Log.e("CardanoTx", "Final CBOR structure assembled")

        val baos = ByteArrayOutputStream()
        CborEncoder(baos).encode(finalArray)

        val result = baos.toByteArray()
        Log.e("CardanoTx", "Final CBOR TX (hex): ${result.toHexString()}")
        return result
    }



    fun removeTag258Recursive(item: DataItem?) {
        if (item == null) return

        if (item.hasTag() && item.tag.value == 258L) {
            Log.e("CardanoTx", "Removed tag(258) from ${item.javaClass.simpleName}")
            item.removeTag()
        }

        when (item) {
            is Map -> {
                for (key in item.keys) {
                    removeTag258Recursive(key)
                    val value = item[key]
                    removeTag258Recursive(value)
                }
            }

            is Array -> {
                for (i in item.dataItems.indices) {
                    removeTag258Recursive(item.dataItems[i])
                }
            }

        }
    }




    /**
     * Require to check that the min-ada-value from Wallet-Core [Cardano.TransactionPlan] is equals real min-ada-value.
     * Because Wallet-Core can hold a fee value from min-ada-value.
     *
     * @param transactionData transaction
     * @param plan        wallet-core transaction input
     */
    private fun checkRequiredMinAdaValue(transactionData: TransactionData, plan: Cardano.TransactionPlan): Boolean {
        transactionData.requireUncompiled()

        return when (val type = transactionData.amount.type) {
            is AmountType.Token -> {
                val minAdaValue = twTxBuilder.calculateMinAdaValueToWithdrawToken(
                    contractAddress = type.token.contractAddress,
                    amount = transactionData.amount.longValue,
                )

                plan.amount < minAdaValue
            }
            else -> false // another types don't use min-ada-value
        }
    }

    private fun checkRemainingAdaBalance(transactionData: TransactionData, plan: Cardano.TransactionPlan) {
        val remainingTokens = getRemainingTokens(transactionData, plan)

        if (remainingTokens.isEmpty()) {
            val minChange = BigDecimal.ONE.movePointRight(decimals).toLong()

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientRemainingBalance,
                condition = plan.change in 1 until minChange,
            )
        } else {
            val minChange = twTxBuilder.calculateMinAdaValueToWithdrawAllTokens(remainingTokens)

            throwIf(
                exception = BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens,
                condition = plan.change == 0L || plan.change in 1 until minChange,
            )
        }
    }

    private fun getRemainingTokens(
        transactionData: TransactionData,
        plan: Cardano.TransactionPlan,
    ): kotlin.collections.Map<Cardano.TokenAmount, Long> {
        transactionData.requireUncompiled()

        return plan.availableTokensList
            .associateWith { tokenAmount ->
                val amount = tokenAmount.amount.toLong()
                val isTransactionToken = transactionData.contractAddress?.matchesCardanoAsset(
                    policyId = tokenAmount.policyId,
                    assetNameHex = tokenAmount.assetNameHex,
                )

                val remainingAmount = if (isTransactionToken == true) {
                    amount - transactionData.amount.longValue
                } else {
                    amount
                }

                remainingAmount
            }
            .filter { it.value > 0 }
    }

    private fun ByteString.toLong(): Long {
        return toByteArray().toHexString().hexToBigDecimal().toLong()
    }

    private fun throwIf(exception: BlockchainSdkError.Cardano, condition: Boolean) {
        if (condition) throw exception
    }

    private companion object {
        const val MISSING_LENGTH_TO_EXTENDED_KEY = 32 * 3
    }
}