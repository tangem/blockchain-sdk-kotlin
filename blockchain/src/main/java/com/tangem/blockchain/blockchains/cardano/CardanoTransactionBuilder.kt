package com.tangem.blockchain.blockchains.cardano

import com.google.protobuf.ByteString
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Cardano
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.math.BigDecimal

// You can decode your CBOR transaction here: https://cbor.me
class CardanoTransactionBuilder {

    private var outputs: List<CardanoUnspentOutput> = emptyList()
    private val coinType: CoinType = CoinType.CARDANO
    private var decimalValue: Int = Blockchain.Cardano.decimals()

    fun update(outputs: List<CardanoUnspentOutput>) {
        this.outputs = outputs
    }

    internal fun buildForSign(transaction: TransactionData): ByteArray {
        val input = buildCardanoSigningInput(transaction)
        val txInputData = input.toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.dataHash.toByteArray()
    }

    @Suppress("MagicNumber")
    internal fun buildForSend(transaction: TransactionData, signatureInfo: SignatureInfo): ByteArray {
        val input = buildCardanoSigningInput(transaction)
        val txInputData = input.toByteArray()

        val signatures = DataVector()
        signatures.add(signatureInfo.signature)

        val publicKeys = DataVector()

        // WalletCore used here `.ed25519Cardano` curve with 128 bytes publicKey.
        // Calculated as: chainCode + secondPubKey + chainCode
        // The number of bytes in a Cardano public key (two ed25519 public key + chain code).
        // We should add dummy chain code in publicKey if we use old 32 byte key to get 128 bytes in total
        val publicKey = if (signatureInfo.publicKey.isExtendedPublicKey()) {
            signatureInfo.publicKey
        } else {
            signatureInfo.publicKey + ByteArray(32 * 3)
        }

        publicKeys.add(publicKey)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            txInputData,
            signatures,
            publicKeys,
        )

        val output = Cardano.SigningOutput.parseFrom(compileWithSignatures)

        if (output.error != Common.SigningError.OK || output.encoded.isEmpty) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return output.encoded.toByteArray()
    }

    fun estimatedFee(transaction: TransactionData): BigDecimal {
        val input = buildCardanoSigningInput(transaction)
        val plan = AnySigner.plan(input, coinType, Cardano.TransactionPlan.parser())

        return BigDecimal(plan.fee)
    }

    @Suppress("MagicNumber")
    private fun buildCardanoSigningInput(transaction: TransactionData): Cardano.SigningInput {
        val utxos = outputs.map { output ->
            Cardano.TxInput.newBuilder()
                .setOutPoint(
                    Cardano.OutPoint.newBuilder()
                        .setTxHash(ByteString.copyFrom(output.transactionHash))
                        .setOutputIndex(output.outputIndex)
                        .build(),
                )
                .setAddress(output.address)
                .setAmount(output.amount)
                .build()
        }

        val input = Cardano.SigningInput.newBuilder()
            .setTransferMessage(
                Cardano.Transfer.newBuilder()
                    .setToAddress(transaction.destinationAddress)
                    .setChangeAddress(transaction.sourceAddress)
                    .setAmount(transaction.amount.longValue!!)
                    .setUseMaxAmount(false),
            )
            .setTtl(190000000)
            .addAllUtxos(utxos)
            .build()

        if (outputs.isEmpty()) {
            throw BlockchainSdkError.CustomError("Outputs are empty")
        }

        val minChange = decimalValue.toBigInteger().toLong()
        val acceptableChangeRange = 1L..minChange

        if (acceptableChangeRange.contains(input.plan.change)) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return input
    }

    @Suppress("MagicNumber")
    private fun ByteArray.isExtendedPublicKey() = this.size == 128
}
