package com.tangem.blockchain.blockchains.solana.alt

import android.os.SystemClock
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.allocate
import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.alt.borsh.BorshDecoder
import com.tangem.blockchain.blockchains.solana.solanaj.model.NewSolanaAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaBlockhashInfo
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.extensions.toHexString
import foundation.metaplex.solana.transactions.SerializeConfig
import foundation.metaplex.solana.transactions.TransactionInstruction
import foundation.metaplex.solana.transactions.WrappedInstruction
import foundation.metaplex.solana.util.Shortvec
import foundation.metaplex.solanapublickeys.PublicKey
import io.ktor.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Shortens legacy (old format) transactions to v0 (new format) transactions using Address Lookup Tables (ALT).
 * Sends v0 transaction to the network.
 */
internal class SolanaTransactionSizeReducer(
    private val walletPubkey: Wallet.PublicKey,
    private val rawTransactionParser: SolanaTransactionParser,
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
) {

    @Suppress("LongMethod")
    /**
     * Always returns empty byte array and sends transaction to the network.
     */
    suspend fun process(signer: TransactionSigner, rawTransaction: ByteArray): ByteArray {
        val parsed = rawTransactionParser.parse(rawTransaction)

        Logger.logTransaction("rawTxInitial: " + rawTransaction.toHexString())

        val existLookupTables = if (parsed.compiledAltTable != null && parsed.compiledAltTable.isNotEmpty()) {
            val tableInfos = mutableListOf<NewSolanaAccountInfo.Value>()
            parsed.compiledAltTable.forEach { compiledAltTable ->
                val info = multiNetworkProvider.performRequest {
                    getTableLookupInfo(org.p2p.solanaj.core.PublicKey(compiledAltTable.account))
                }.successOr {
                    Logger.logTransaction("fail to get lookup table info: $it")
                    null
                }
                info?.let { tableInfos.add(info) }
            }

            tableInfos.map {
                val binaryTable = it.data[0].decodeBase64Bytes()
                AddressLookupTableState.fromReader(BorshDecoder(binaryTable))
            }
        } else {
            Logger.logTransaction("compiledAltTable is null")
            null
        }

        Logger.logTransaction("lookupTables count: ${existLookupTables?.size}")

        val existTablesAddresses = existLookupTables?.takeAddressesForTx(parsed) ?: emptyList()
        val allAddresses = parsed.staticAccountAddresses + existTablesAddresses.map { it.address }

        val transactionInstructions = rawTransactionParser.convertCompiledToTransactionInstructions(
            compiledInstructions = parsed.compiledInstructions,
            allAccountAddresses = allAddresses,
            requiredSignatures = parsed.messageHeader.numRequiredSignatures.readCompactU16(),
            readonlySignedAccounts = parsed.messageHeader.numReadonlySignedAccounts.readCompactU16(),
            readonlyUnsignedAccounts = parsed.messageHeader.numReadonlyUnsignedAccounts.readCompactU16(),
        ).patchComputeBudgetProgramInstructions()

        val (newStaticAddresses, newAltAddresses) = splitAddressesForV0(
            payer = parsed.payer,
            instructions = transactionInstructions,
            writableAltAddresses = parsed.writableAltAddresses,
            readonlyAltAddresses = parsed.readonlyAltAddresses,
        )

        val networkParams = multiNetworkProvider.performRequest { getLatestBlockhashInfo() }
            .successOr<SolanaBlockhashInfo> {
                Logger.logTransaction("failed to get latest blockhash info: $it")
                return byteArrayOf()
            }

        val lookupAltQueue = RemovableQueue(newAltAddresses.chunked(size = ALT_ADDRESS_CHUNK_SIZE))
        Logger.logTransaction("createAndExtendLookupTableTransaction: chunks - ${lookupAltQueue.size()}")

        val (createAltTransaction, tableAddress) = ALTCreateTransactionBuilder.createAndExtendLookupTableTransaction(
            authority = PublicKey(walletPubkey.blockchainKey),
            payer = PublicKey(walletPubkey.blockchainKey),
            recentSlot = networkParams.slot.toULong(),
            recentBlockhash = networkParams.blockhash,
            addresses = lookupAltQueue.takeFirst().map { PublicKey(it.address) },
        )

        val serializedAltTx = createAltTransaction.compileMessage().serialize()

        Logger.logTransaction("sign alt transaction")
        val createAltTransactionSignature = signer.sign(
            hash = serializedAltTx,
            publicKey = walletPubkey,
        ).successOr {
            Logger.logTransaction("fail to sign ALT creation transaction: $it")
            return byteArrayOf()
        }

        createAltTransaction.addSignature(
            pubkey = PublicKey(walletPubkey.blockchainKey),
            signature = createAltTransactionSignature,
        )

        sendTransaction(
            signedTransaction = createAltTransaction.serialize(SerializeConfig(verifySignatures = false)),
            startSendingTimestamp = SystemClock.elapsedRealtime(),
        )

        while (!lookupAltQueue.isEmpty()) {
            Logger.logTransaction("extend ALT transaction: ")
            val networkParams = multiNetworkProvider.performRequest { getLatestBlockhashInfo() }
                .successOr<SolanaBlockhashInfo> {
                    Logger.logTransaction("failed to get latest blockhash info: $it")
                    return byteArrayOf()
                }

            val addressesChunk = lookupAltQueue.takeFirst()
            val extendTx = ALTExtendTransactionBuilder.extendLookupTableTransaction(
                lookupTable = tableAddress,
                authority = PublicKey(walletPubkey.blockchainKey),
                payer = PublicKey(walletPubkey.blockchainKey),
                addresses = addressesChunk.map { PublicKey(it.address) },
                recentBlockhash = networkParams.blockhash,
            )

            // 7. Sign and send ALT creation transaction
            val serializedExtendAltTx = extendTx.compileMessage().serialize()

            val extendAltTransactionSignature = signer.sign(
                hash = serializedExtendAltTx,
                publicKey = walletPubkey,
            ).successOr {
                Logger.logTransaction("fail to sign ALT extend transaction: $it")
                return byteArrayOf()
            }

            extendTx.addSignature(
                pubkey = PublicKey(walletPubkey.blockchainKey),
                signature = extendAltTransactionSignature,
            )

            sendTransaction(
                signedTransaction = extendTx.serialize(SerializeConfig(verifySignatures = false)),
                startSendingTimestamp = SystemClock.elapsedRealtime(),
            )
        }

        val recentBlockhashInfo = multiNetworkProvider.performRequest { getLatestBlockhashInfo() }
            .successOr<SolanaBlockhashInfo> {
                Logger.logTransaction("failed to get latest blockhash info: $it")
                return byteArrayOf()
            }

        val v0Message = buildV0Message(
            instructions = transactionInstructions,
            newRecentBlockhash = recentBlockhashInfo.blockhash.decodeBase58() ?: byteArrayOf(),
            altAddress = tableAddress.toByteArray(),
            existAltAddresses = existTablesAddresses,
            existAltCompiled = parsed.compiledAltTable ?: emptyList(),
            staticAndAltAddresses = newStaticAddresses to newAltAddresses,
        )

        Logger.logTransaction("v0 message: " + v0Message.toHexString())

        val signedMessage = signer.sign(v0Message, walletPubkey).successOr {
            Logger.logTransaction("fail to sign v0 message: $it")
            return byteArrayOf()
        }

        val v0Tx = buildV0Transaction(
            signature = signedMessage,
            messageV0 = v0Message,
        )

        Logger.logTransaction("v0Tx signed: " + v0Tx.toHexString())

        sendTransaction(v0Tx, SystemClock.elapsedRealtime()).successOr {
            Logger.logTransaction("fail to send v0 tx: $it")
            return byteArrayOf()
        }

        return byteArrayOf()
    }

    fun splitAddressesForV0(
        payer: ByteArray,
        instructions: List<TransactionInstruction>,
        writableAltAddresses: List<ByteArray>,
        readonlyAltAddresses: List<ByteArray>,
    ): Pair<List<AltAddress>, List<AltAddress>> {
        val programAddresses = instructions.map { AltAddress(it.programId.toByteArray(), false) }
        val signerAddresses = instructions
            .flatMap { it.keys }
            .filter { it.isSigner }
            .filterNot { it.publicKey.toByteArray() == payer }
            .map { AltAddress(it.publicKey.toByteArray(), true) }

        val staticSet = (listOf(AltAddress(payer, true)) + programAddresses + signerAddresses).distinct()
        val staticAddressSet = staticSet.map { it.address }

        val altWritable = writableAltAddresses
            .filter { it !in staticAddressSet }
            .map { AltAddress(it, true) }
        val altReadonly = readonlyAltAddresses
            .filter { it !in staticAddressSet }
            .map { AltAddress(it, false) }
        val altList = altWritable + altReadonly

        return staticSet.toList() to altList
    }

    private suspend fun sendTransaction(
        signedTransaction: ByteArray,
        startSendingTimestamp: Long,
    ): Result<TransactionSendResult> {
        val sendResults = coroutineScope {
            multiNetworkProvider.providers
                .map { provider ->
                    async {
                        provider.sendTransaction(signedTransaction, startSendingTimestamp)
                    }
                }
                .awaitAll()
        }
        val firstSuccessResult = sendResults
            .filterIsInstance<Result.Success<String>>()
            .firstOrNull()

        if (firstSuccessResult != null) {
            return Result.Success(TransactionSendResult(firstSuccessResult.data))
        }

        val error = sendResults
            .filterIsInstance<Result.Failure>()
            .firstOrNull()
            ?.error
            ?: BlockchainSdkError.FailedToSendException
        return Result.Failure(error)
    }

    @Suppress("MagicNumber", "LongParameterList")
    fun buildV0Message(
        instructions: List<TransactionInstruction>,
        newRecentBlockhash: ByteArray, // 32 байта
        altAddress: ByteArray, // 32 байта — ALT address
        existAltAddresses: List<AltAddress>,
        staticAndAltAddresses: Pair<List<AltAddress>, List<AltAddress>>,
        existAltCompiled: List<CompiledAltTable>,
    ): ByteArray {
        // all addresses structure
        // [static] + [existAlt] + [newAlt]
        val staticAddresses = staticAndAltAddresses.first
        val newAltAddresses = staticAndAltAddresses.second
        val allAddresses = staticAddresses.map { it.address } +
            existAltAddresses.filter { it.isWritable }.map { it.address } +
            newAltAddresses.filter { it.isWritable }.map { it.address } +
            existAltAddresses.filter { !it.isWritable }.map { it.address } +
            newAltAddresses.filter { !it.isWritable }.map { it.address }

        val versionPrefix: Byte = 0x80.toByte() // (1 << 7) | 0 → V0
        val requiredSignatures: Byte = 1
        val readonlySigned: Byte = 0
        val readonlyUnsigned: Byte = staticAddresses.filter { !it.isWritable }.size.toByte()

        // -----------------------------
        val newHeader = byteArrayOf(requiredSignatures, readonlySigned, readonlyUnsigned)

        // -----------------------------
        val accounts = ByteArray(1 + 32 * staticAddresses.size) // compact-u16 = 0x01 + один pubkey
        accounts[0] = staticAddresses.size.toByte() // 0x01
        staticAddresses.forEachIndexed { i, addr -> addr.address.copyInto(accounts, 1 + i * 32) }

        // 4. Blockhash
        val blockhash = newRecentBlockhash // уже 32 байта

        val instructionsWrapped = instructions.mapIndexed { i, instruction ->
            val keyIndicesCount = Shortvec.encodeLength(instruction.keys.count())
            val dataCount = Shortvec.encodeLength(instruction.data.count())

            WrappedInstruction(
                programIdIndex = allAddresses.safeIndexOf(instruction.programId.toByteArray()).toByte(),
                keyIndicesCount = keyIndicesCount,
                keyIndices = instruction.keys.mapIndexed { ind, key ->
                    allAddresses.safeIndexOf(key.publicKey.toByteArray()).toByte()
                }.toByteArray(),
                dataLength = dataCount,
                data = instruction.data,
            )
        }

        val compiledInstructionsLength = instructionsWrapped.sumOf { it.length }
        val instructionCount = Shortvec.encodeLength(instructionsWrapped.size)
        val bufferSize = instructionCount.size + compiledInstructionsLength

        val buffer = PlatformBuffer.allocate(size = bufferSize)
        buffer.writeBytes(instructionCount)
        for (instruction in instructionsWrapped) {
            buffer.writeByte(instruction.programIdIndex)
            buffer.writeBytes(instruction.keyIndicesCount)
            buffer.writeBytes(instruction.keyIndices)
            buffer.writeBytes(instruction.dataLength)
            buffer.writeBytes(instruction.data)
        }
        buffer.resetForRead()
        val instructionsSerialized = buffer.readByteArray(bufferSize)

        // -----------------------------
        // 5. Message
        val message = newHeader + accounts + blockhash + instructionsSerialized

        // -----------------------------
        // 6. ALT Entry
        val altEntry = mutableListOf<Byte>()

        altEntry.addExistLookupTables(existAltCompiled)

        // ALT address
        altEntry += altAddress.toList()

        val writableAlt = newAltAddresses.filter { it.isWritable }
        // Writable count
        altEntry += writableAlt.size.toByte()
        for (i in 0 until writableAlt.size) {
            altEntry += i.toByte()
        }

        val readonlyAltAddresses = newAltAddresses.filter { !it.isWritable }
        // Readonly count
        val readonlyCount = readonlyAltAddresses.size
        altEntry += readonlyCount.toByte()
        for (i in 0 until readonlyCount) {
            altEntry += (writableAlt.size + i).toByte()
        }

        // -----------------------------
        val finalTx = byteArrayOf(versionPrefix) +
            message +
            (existAltCompiled.size + 1/*const table*/).toByte() +
            altEntry.toByteArray()

        return finalTx
    }

    fun buildV0Transaction(signature: ByteArray, messageV0: ByteArray): ByteArray {
        val finalTx = byteArrayOf(0x01) + // Signature count
            signature +
            messageV0

        return finalTx
    }

    private companion object {
        const val ALT_ADDRESS_CHUNK_SIZE = 14
    }
}

private const val COMPUTE_BUDGET_PROGRAM_ID = "ComputeBudget111111111111111111111111111111"
private const val CU_LIMIT_INSTRUCTION_SIZE = 5 // 1 byte discriminator + 4 bytes limit
private const val CU_LIMIT_INSTRUCTION_BYTES = 4
private const val BUDGET_INCREASE_PERCENTAGE = 120 // Increase budget by 20%
private const val PERCENTAGE_BASE = 100

@Suppress("MagicNumber")
private fun List<TransactionInstruction>.patchComputeBudgetProgramInstructions(): List<TransactionInstruction> {
    return this.map {
        if (it.programId.toBase58() == COMPUTE_BUDGET_PROGRAM_ID && it.data.size == CU_LIMIT_INSTRUCTION_SIZE) {
            val discriminator = it.data[0]
            val currentBudget = ByteBuffer.wrap(it.data.drop(1).toByteArray())
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
            val increasedBudget = currentBudget * BUDGET_INCREASE_PERCENTAGE / PERCENTAGE_BASE
            val increasedBudgetBytes = ByteBuffer.allocate(CU_LIMIT_INSTRUCTION_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(increasedBudget)
                .array()
            it.copy(
                data = byteArrayOf(discriminator) + increasedBudgetBytes,
            )
        } else {
            it
        }
    }
}

private fun List<AddressLookupTableState>.takeAddressesForTx(data: TransactionRawData): List<AltAddress> {
    val writeableAddresses = mutableListOf<AltAddress>()
    val readonlyAddresses = mutableListOf<AltAddress>()
    data.compiledAltTable?.forEachIndexed { i, altTable ->
        val tableState = this[i]
        altTable.writableIndexes.forEach {
            writeableAddresses.add(AltAddress(tableState.addresses[it], true))
        }
        altTable.readonlyIndexes.forEach {
            readonlyAddresses.add(AltAddress(tableState.addresses[it], false))
        }
    }
    return writeableAddresses + readonlyAddresses
}

private fun MutableList<Byte>.addExistLookupTables(tables: List<CompiledAltTable>) {
    tables.forEach { table ->
        this += table.account.toList()

        // add writable
        val writable = table.writableIndexes
        this += writable.size.toByte()
        writable.forEach {
            this += it.toByte()
        }

        // add readonly
        val readonly = table.readonlyIndexes
        this += readonly.size.toByte()
        readonly.forEach {
            this += it.toByte()
        }
    }
}

private fun <T> List<T>.safeIndexOf(item: T): Int {
    val i = this.indexOf(item)
    if (i == -1) {
        error("Item not found in the list: $item")
    }
    return i
}