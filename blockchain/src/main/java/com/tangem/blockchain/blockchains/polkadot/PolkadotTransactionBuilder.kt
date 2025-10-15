package com.tangem.blockchain.blockchains.polkadot

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.polkadot.extensions.makeEraFromBlockNumber
import com.tangem.blockchain.blockchains.polkadot.models.PolkadotCompiledTransaction
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.hexToBigInteger
import com.tangem.blockchain.extensions.hexToInt
import com.tangem.blockchain.network.moshi
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.sign
import com.tangem.operations.sign.SignData
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
        Blockchain.PolkadotTestnet,
        Blockchain.Kusama,
        -> "0a00".hexToBytes()
        Blockchain.EnergyWebX,
        -> "0a00".hexToBytes()
        else -> error(
            "${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}",
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val compiledTransactionAdapter by lazy { moshi.adapter<PolkadotCompiledTransaction>() }

    fun buildForSign(destinationAddress: String, amount: Amount, context: ExtrinsicContext): ByteArray {
        val buffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(buffer)

        encodeCall(codecWriter, amount, Address.from(destinationAddress), context.runtimeVersion)
        encodeEraNonceTip(codecWriter, context)
        encodeAssetId(codecWriter)
        encodeCheckMetadataHashMode(codecWriter, context.runtimeVersion)

        codecWriter.writeUint32(context.runtimeVersion)
        codecWriter.writeUint32(context.txVersion)
        codecWriter.writeUint256(context.genesis.bytes)
        if (context.era.isImmortal) {
            codecWriter.writeUint256(context.genesis.bytes)
        } else {
            codecWriter.writeUint256(context.eraBlockHash.bytes)
        }

        encodeCheckMetadataHash(codecWriter, context.runtimeVersion)

        return buffer.toByteArray()
    }

    fun buildForSignCompiled(transaction: TransactionData.Compiled): ByteArray {
        val compiledTransaction = (transaction.value as? TransactionData.Compiled.Data.RawString)?.data
            ?: error("Compiled transaction must be in hex format")

        val parsed = compiledTransactionAdapter.fromJson(compiledTransaction)
            ?: error("Unable to parse compiled transaction")
        val tx = parsed.tx

        val buffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(buffer)

        codecWriter.writeByteArray(tx.method.hexToBytes())
        encodeEraNonceTip(codecWriter, tx)
        encodeAssetId(codecWriter)
        encodeCheckMetadataHashMode(codecWriter, tx.specVersion.hexToInt())
        codecWriter.writeUint32(tx.specVersion.hexToInt())
        codecWriter.writeUint32(tx.transactionVersion.hexToInt())
        codecWriter.writeByteArray(tx.genesisHash.hexToBytes())
        codecWriter.writeByteArray(tx.blockHash.hexToBytes())
        encodeCheckMetadataHash(codecWriter, tx.specVersion.hexToInt())

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
        encodeAssetId(codecWriter)
        encodeCheckMetadataHashMode(codecWriter, context.runtimeVersion)
        encodeCall(codecWriter, amount, Address.from(destinationAddress), context.runtimeVersion)

        val prefixBuffer = ByteArrayOutputStream()
        ScaleCodecWriter(prefixBuffer).write(ScaleCodecWriter.COMPACT_UINT, txBuffer.size())

        return prefixBuffer.toByteArray() + txBuffer.toByteArray()
    }

    @Suppress("MagicNumber")
    fun buildForSendCompiled(transaction: TransactionData.Compiled, signedPayload: ByteArray): ByteArray {
        val compiledTransaction = (transaction.value as? TransactionData.Compiled.Data.RawString)?.data
            ?: error("Compiled transaction must be in hex format")

        val parsed = compiledTransactionAdapter.fromJson(compiledTransaction)
            ?: error("Unable to parse compiled transaction")

        val tx = parsed.tx

        val type = Extrinsic.TYPE_BIT_SIGNED + (Extrinsic.TYPE_UNMASK_VERSION and 4)
        val hash512 = Hash512(signedPayload)
        val signature = Extrinsic.ED25519Signature(hash512)

        val buffer = ByteArrayOutputStream()
        val codecWriter = ScaleCodecWriter(buffer)

        codecWriter.writeByte(type)
        encodeAddress(codecWriter, Address.from(tx.address), tx.specVersion.hexToInt())
        codecWriter.writeByte(signature.type.code)
        codecWriter.writeByteArray(signature.value.bytes)
        encodeEraNonceTip(codecWriter, tx)
        encodeAssetId(codecWriter)
        encodeCheckMetadataHashMode(codecWriter, tx.specVersion.hexToInt())
        codecWriter.writeByteArray(tx.method.hexToBytes())

        val prefixBuffer = ByteArrayOutputStream()
        ScaleCodecWriter(prefixBuffer).write(ScaleCodecWriter.COMPACT_UINT, buffer.size())

        return prefixBuffer.toByteArray() + buffer.toByteArray()
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

    private fun encodeEraNonceTip(codecWriter: ScaleCodecWriter, compiledTx: PolkadotCompiledTransaction.Inner) {
        val era = makeEraFromBlockNumber(compiledTx.blockNumber.hexToBigInteger().toLong())
        codecWriter.write(EraWriter(), era.toInteger())
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, compiledTx.nonce.hexToBigInteger())
        codecWriter.write(ScaleCodecWriter.COMPACT_BIGINT, compiledTx.tip.hexToBigInteger())
    }

    private fun encodeCheckMetadataHashMode(codecWriter: ScaleCodecWriter, specVersion: Int) {
        if (shouldUseCheckMetadataHash(specVersion = specVersion)) {
            codecWriter.write(ScaleCodecWriter.BOOL, false)
        }
    }

    private fun encodeCheckMetadataHash(codecWriter: ScaleCodecWriter, specVersion: Int) {
        if (shouldUseCheckMetadataHash(specVersion = specVersion)) {
            codecWriter.writeByte(0x0.toByte())
        }
    }

    private fun shouldUseCheckMetadataHash(specVersion: Int): Boolean {
        val metadataSpecVersion = SUPPORTED_CHECK_METADATA_HASH_BLOCKCHAINS[blockchain] ?: return false
        return specVersion >= metadataSpecVersion
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

    private fun encodeAssetId(codecWriter: ScaleCodecWriter) {
        if (isInAssetHubList(blockchain)) {
            codecWriter.writeByte(0x0.toByte())
        }
    }

    private fun isInAssetHubList(blockchain: Blockchain): Boolean {
        return when (blockchain) {
            Blockchain.Kusama -> true
            else -> false
        }
    }

    private companion object {
        const val POLKA_RAW_ADDRESS_RUNTIME_VERSION = 28
        const val KUSAMA_RAW_ADDRESS_RUNTIME_VERSION = 2028

        const val USE_CHECK_METADATA_HASH_SPEC_VERSION_POLKADOT = 1002005
        const val USE_CHECK_METADATA_HASH_SPEC_VERSION_BITTENSOR = 198
        const val USE_CHECK_METADATA_HASH_SPEC_VERSION_ENERGY_WEB_X = 77

        val SUPPORTED_CHECK_METADATA_HASH_BLOCKCHAINS = mapOf(
            Blockchain.Polkadot to USE_CHECK_METADATA_HASH_SPEC_VERSION_POLKADOT,
            Blockchain.PolkadotTestnet to USE_CHECK_METADATA_HASH_SPEC_VERSION_POLKADOT,
            Blockchain.Kusama to USE_CHECK_METADATA_HASH_SPEC_VERSION_POLKADOT,
            Blockchain.Bittensor to USE_CHECK_METADATA_HASH_SPEC_VERSION_BITTENSOR,
            Blockchain.EnergyWebX to USE_CHECK_METADATA_HASH_SPEC_VERSION_ENERGY_WEB_X,
        )
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

    override suspend fun multiSign(
        dataToSign: List<SignData>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<Map<ByteArray, ByteArray>> {
        return CompletionResult.Success(
            dataToSign.associate {
                it.hash to it.hash.sign(privateKey, EllipticCurve.Ed25519)
            },
        )
    }
}