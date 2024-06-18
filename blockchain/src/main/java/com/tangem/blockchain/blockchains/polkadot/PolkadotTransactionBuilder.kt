package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
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
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.Hash512
import java.io.ByteArrayOutputStream
import java.math.BigInteger

/**
[REDACTED_AUTHOR]
 */
class PolkadotTransactionBuilder(private val blockchain: Blockchain) {

    private val decimals = blockchain.decimals()

    private val balanceTransferCallIndex: ByteArray = when (blockchain) {
        Blockchain.Polkadot,
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet,
        Blockchain.Joystream,
        Blockchain.Bittensor,
        -> "0500".hexToBytes()
        Blockchain.PolkadotTestnet, Blockchain.Kusama -> "0400".hexToBytes()
        else -> error(
            "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}",
        )
    }

    fun buildForSign(destinationAddress: String, amount: Amount, context: ExtrinsicContext): ByteArray {
        val buffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(buffer)

        encodeCall(codecWriter, amount, Address.from(destinationAddress), context.runtimeVersion)
        encodeEraNonceTip(codecWriter, context)

        encodeCheckMetadataHashMode(codecWriter)

        codecWriter.writeUint32(context.runtimeVersion)
        codecWriter.writeUint32(context.txVersion)
        codecWriter.writeUint256(context.genesis.bytes)
        if (context.era.isImmortal) {
            codecWriter.writeUint256(context.genesis.bytes)
        } else {
            codecWriter.writeUint256(context.eraBlockHash.bytes)
        }

        encodeCheckMetadataHash(codecWriter)

        return buffer.toByteArray()
    }

    @Suppress("MagicNumber")
    fun buildForSend(
        sourceAddress: String,
        destinationAddress: String,
        amount: Amount,
        context: ExtrinsicContext,
        signedPayload: ByteArray,
    ): ByteArray {
        val txBuffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(txBuffer)

        val type = Extrinsic.TYPE_BIT_SIGNED + (Extrinsic.TYPE_UNMASK_VERSION and 4)
        codecWriter.writeByte(type)
        encodeAddress(codecWriter, Address.from(sourceAddress), context.runtimeVersion)

        val hash512 = Hash512(signedPayload)
        val signature = Extrinsic.ED25519Signature(hash512)
        codecWriter.writeByte(signature.type.code)
        codecWriter.writeByteArray(signature.value.bytes)

        encodeEraNonceTip(codecWriter, context)
        encodeCheckMetadataHashMode(codecWriter)
        encodeCall(codecWriter, amount, Address.from(destinationAddress), context.runtimeVersion)

        val prefixBuffer = ByteArrayOutputStream()
        ScaleCodecWriter(prefixBuffer).write(ScaleCodecWriter.COMPACT_UINT, txBuffer.size())

        return prefixBuffer.toByteArray() + txBuffer.toByteArray()
    }

    private fun encodeCall(
        codecWriter: ScaleCodecWriter,
        amount: Amount,
        destinationAddress: Address,
        runtimeVersion: Int,
    ) {
        codecWriter.writeByteArray(balanceTransferCallIndex)
        encodeAddress(codecWriter, destinationAddress, runtimeVersion)

        val amountToSend = amount.value?.movePointRight(decimals)?.toBigInteger() ?: BigInteger.ZERO
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, amountToSend)
    }

    private fun encodeEraNonceTip(codecWriter: ScaleCodecWriter, context: ExtrinsicContext) {
        codecWriter.write(EraWriter(), context.era.toInteger())
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, BigInteger.valueOf(context.nonce))
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, context.tip.value)
    }

    private fun encodeCheckMetadataHashMode(codecWriter: ScaleCodecWriter) {
        if (blockchain == Blockchain.Kusama || blockchain == Blockchain.PolkadotTestnet) {
            codecWriter.write(ScaleCodecWriter.BOOL, false)
        }
    }

    private fun encodeCheckMetadataHash(codecWriter: ScaleCodecWriter) {
        if (blockchain == Blockchain.Kusama || blockchain == Blockchain.PolkadotTestnet) {
            codecWriter.writeByte(0x0.toByte())
        }
    }

    private fun encodeAddress(codecWriter: ScaleCodecWriter, address: Address, runtimeVersion: Int) {
        if (needEncodeRawAddress(blockchain, runtimeVersion)) {
            codecWriter.writeByteArray(address.pubkey)
        } else {
            codecWriter.write(MultiAddressWriter(), address.toUnionAddress())
        }
    }

    private fun needEncodeRawAddress(blockchain: Blockchain, runtimeVersion: Int): Boolean {
        return when (blockchain) {
            Blockchain.Polkadot -> runtimeVersion < POLKA_RAW_ADDRESS_RUNTIME_VERSION
            Blockchain.Kusama -> runtimeVersion < KUSAMA_RAW_ADDRESS_RUNTIME_VERSION
            Blockchain.Joystream -> true
            else -> false
        }
    }

    private companion object {
        const val POLKA_RAW_ADDRESS_RUNTIME_VERSION = 28
        const val KUSAMA_RAW_ADDRESS_RUNTIME_VERSION = 2028
    }
}

private fun Address.toUnionAddress(): UnionValue<MultiAddress> {
    return MultiAddress.AccountID.from(this)
}

internal class DummyPolkadotTransactionSigner : TransactionSigner {

    private val privateKey = CryptoUtils.generateRandomBytes(32)

    override suspend fun sign(hashes: List<ByteArray>, publicKey: Wallet.PublicKey): CompletionResult<List<ByteArray>> {
        /* todo use Ed25519Slip0010 or Ed25519 depends on wallet manager
         * [REDACTED_JIRA]
         */
        val signResults = hashes.map { it.sign(privateKey, EllipticCurve.Ed25519) }
        return CompletionResult.Success(signResults)
    }

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
        return CompletionResult.Success(hash.sign(privateKey, EllipticCurve.Ed25519))
    }
}