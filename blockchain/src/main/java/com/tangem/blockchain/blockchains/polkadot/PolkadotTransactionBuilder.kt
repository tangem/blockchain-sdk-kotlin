package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.polkaj.extensions.isKusama
import com.tangem.blockchain.blockchains.polkadot.polkaj.extensions.isPolkadot
import com.tangem.blockchain.blockchains.polkadot.polkaj.extensions.isWestend
import com.tangem.blockchain.blockchains.polkadot.polkaj.extensions.toDot
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.sign
import io.emeraldpay.polkaj.scale.ScaleCodecWriter
import io.emeraldpay.polkaj.scale.UnionValue
import io.emeraldpay.polkaj.scaletypes.EraWriter
import io.emeraldpay.polkaj.scaletypes.Extrinsic
import io.emeraldpay.polkaj.scaletypes.MultiAddress
import io.emeraldpay.polkaj.scaletypes.MultiAddressWriter
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.DotAmount
import io.emeraldpay.polkaj.types.Hash512
import java.io.ByteArrayOutputStream
import java.math.BigInteger

/**
 * Created by Anton Zhilenkov on 08/08/2022.
 */
class PolkadotTransactionBuilder(
    private val network: SS58Type.Network,
) {

    private val balanceTransferCallIndex: ByteArray = when {
        network.isPolkadot() -> "0500".hexToBytes()
        network.isWestend() -> "0400".hexToBytes()
        network.isKusama() -> "0400".hexToBytes()
        else -> throw BlockchainSdkError.UnsupportedOperation()
    }

    fun buildForSign(
        destinationAddress: Address,
        amount: Amount,
        context: ExtrinsicContext,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(buffer)

        encodeCall(codecWriter, amount, destinationAddress)
        encodeEraNonceTip(codecWriter, context)

        codecWriter.writeUint32(context.runtimeVersion)
        codecWriter.writeUint32(context.txVersion)
        codecWriter.writeUint256(context.genesis.bytes)
        if (context.era.isImmortal) {
            codecWriter.writeUint256(context.genesis.bytes)
        } else {
            codecWriter.writeUint256(context.eraBlockHash.bytes)
        }

        return buffer.toByteArray()
    }

    fun buildForSend(
        sourceAddress: Address,
        destinationAddress: Address,
        amount: Amount,
        context: ExtrinsicContext,
        signedPayload: ByteArray
    ): ByteArray {
        val txBuffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(txBuffer)

        val type = Extrinsic.TYPE_BIT_SIGNED + (Extrinsic.TYPE_UNMASK_VERSION and 4)
        codecWriter.writeByte(type)
        codecWriter.write(MultiAddressWriter(), sourceAddress.toUnionAddress())

        val hash512 = Hash512(signedPayload)
        val signature = Extrinsic.ED25519Signature(hash512)
        codecWriter.writeByte(signature.type.code)
        codecWriter.writeByteArray(signature.value.bytes)

        encodeEraNonceTip(codecWriter, context)
        encodeCall(codecWriter, amount, destinationAddress)

        val prefixBuffer = ByteArrayOutputStream()
        ScaleCodecWriter(prefixBuffer).write(ScaleCodecWriter.COMPACT_UINT, txBuffer.size())

        val transactionData = prefixBuffer.toByteArray() + txBuffer.toByteArray()
        return transactionData
    }

    private fun encodeCall(codecWriter: ScaleCodecWriter, amount: Amount, destinationAddress: Address) {
        codecWriter.writeByteArray(balanceTransferCallIndex)
        codecWriter.write(MultiAddressWriter(), destinationAddress.toUnionAddress())

        val amountToSend = (amount.value?.toDot(network) ?: DotAmount.ZERO).value
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, amountToSend)
    }

    private fun encodeEraNonceTip(codecWriter: ScaleCodecWriter, context: ExtrinsicContext) {
        codecWriter.write(EraWriter(), context.era.toInteger())
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, BigInteger.valueOf(context.nonce))
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, context.tip.value)
    }
}

private fun Address.toUnionAddress(): UnionValue<MultiAddress> {
    return MultiAddress.AccountID.from(this)
}

internal class DummyPolkadotTransactionSigner : TransactionSigner {

    private val privateKey = CryptoUtils.generateRandomBytes(32)

    override suspend fun sign(hashes: List<ByteArray>, publicKey: Wallet.PublicKey): CompletionResult<List<ByteArray>> {
        val signResults = hashes.map { it.sign(privateKey, EllipticCurve.Ed25519) }
        return CompletionResult.Success(signResults)
    }

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
        return CompletionResult.Success(hash.sign(privateKey, EllipticCurve.Ed25519))
    }
}