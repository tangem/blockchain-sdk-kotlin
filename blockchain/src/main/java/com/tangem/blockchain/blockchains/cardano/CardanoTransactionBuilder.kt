package com.tangem.blockchain.blockchains.cardano

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.trustWalletCoinType
import com.tangem.common.extensions.toByteArray
import wallet.core.java.AnySigner
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Cardano
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput
import java.math.BigDecimal

// You can decode your CBOR transaction here: https://cbor.me
internal class CardanoTransactionBuilder(wallet: Wallet) {

    private val coinType: CoinType = wallet.blockchain.trustWalletCoinType
    private val decimal: Int = wallet.blockchain.decimals()

    private var outputs: List<CardanoUnspentOutput> = emptyList()

    fun update(outputs: List<CardanoUnspentOutput>) {
        this.outputs = outputs
    }

    fun buildForSign(transaction: TransactionData): ByteArray {
        val input = buildCardanoSigningInput(transaction)
        val txInputData = input.toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(transaction: TransactionData, signatureInfo: SignatureInfo): ByteArray {
        val input = buildCardanoSigningInput(transaction)
        val txInputData = input.toByteArray()

        val signatures = DataVector()
        signatures.add(signatureInfo.signature)

        val publicKeys = DataVector()

        // WalletCore used here `.ed25519Cardano` curve with 128 bytes publicKey.
        // Calculated as: chainCode + secondPubKey + chainCode
        // The number of bytes in a Cardano public key (two ed25519 public key + chain code).
        // We should add dummy chain code in publicKey if we use old 32 byte key to get 128 bytes in total
        val publicKey = if (CardanoUtils.isExtendedPublicKey(signatureInfo.publicKey)) {
            signatureInfo.publicKey
        } else {
            signatureInfo.publicKey + ByteArray(MISSING_LENGTH_TO_EXTENDED_KEY)
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

    private fun buildCardanoSigningInput(transaction: TransactionData): Cardano.SigningInput {
        if (outputs.isEmpty()) throw BlockchainSdkError.CustomError("Outputs are empty")

        val transferWithoutAmount = transaction.createTransfer()
        val inputWithoutAmount = createSigningInput(transfer = transferWithoutAmount)

        val transfer = when (val type = transaction.amount.type) {
            is AmountType.Coin -> {
                transferWithoutAmount.setCoinAmount(
                    change = inputWithoutAmount.plan.change,
                    amount = transaction.amount.longValue ?: 0L,
                )
            }
            is AmountType.Token -> transferWithoutAmount.setTokenAmount(token = type.token)
            is AmountType.Reserve -> throw BlockchainSdkError.CustomError("Reserve amount is not supported")
        }

        return inputWithoutAmount.toBuilder()
            .setTransferMessage(transfer)
            .build()
    }

    private fun TransactionData.createTransfer(): Cardano.Transfer {
        return Cardano.Transfer.newBuilder()
            .setToAddress(destinationAddress)
            .setChangeAddress(sourceAddress)
            .setUseMaxAmount(false)
            .build()
    }

    private fun createSigningInput(transfer: Cardano.Transfer): Cardano.SigningInput {
        return Cardano.SigningInput.newBuilder()
            .setTransferMessage(transfer)
            .setTtl(TRANSACTION_TTL)
            .addAllUtxos(outputs.map(::createTxInput))
            .build()
    }

    private fun createTxInput(output: CardanoUnspentOutput): Cardano.TxInput {
        return Cardano.TxInput.newBuilder()
            .setOutPoint(
                Cardano.OutPoint.newBuilder()
                    .setTxHash(ByteString.copyFrom(output.transactionHash))
                    .setOutputIndex(output.outputIndex)
                    .build(),
            )
            .setAddress(output.address)
            .setAmount(output.amount)
            .apply {
                if (output.assets.isNotEmpty()) {
                    addAllTokenAmount(output.assets.map(::createTokenAmount))
                }
            }
            .build()
    }

    private fun Cardano.Transfer.setCoinAmount(change: Long, amount: Long): Cardano.Transfer {
        // Min change is 1 ADA. It's also a dust value.
        val minChange = decimal.toBigInteger().toLong()

        if (change in 1 until minChange) throw BlockchainSdkError.FailedToBuildTx

        return this
            .toBuilder()
            .setAmount(amount)
            .build()
    }

    private fun Cardano.Transfer.setTokenAmount(token: Token): Cardano.Transfer {
        val asset = outputs
            .flatMap(CardanoUnspentOutput::assets)
            .firstOrNull { token.contractAddress.startsWith(prefix = it.policyID) }
            ?: throw BlockchainSdkError.FailedToBuildTx

        val tokenBundle = Cardano.TokenBundle.newBuilder()
            .addToken(createTokenAmount(asset))
            .build()

        return this
            .toBuilder()
            .setTokenAmount(tokenBundle)
            .build()
    }

    private fun createTokenAmount(asset: CardanoUnspentOutput.Asset): Cardano.TokenAmount {
        return Cardano.TokenAmount.newBuilder()
            .setPolicyId(asset.policyID)
            .setAssetNameHex(asset.assetNameHex)
            .setAmount(ByteString.copyFrom(asset.amount.toByteArray()))
            .build()
    }

    private companion object {

        const val MISSING_LENGTH_TO_EXTENDED_KEY = 32 * 3
        const val TRANSACTION_TTL = 190000000L
    }
}