package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.models.KoinosAccountNonce
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosProtocol
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.successOr
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.kethereum.extensions.removeLeadingZero
import java.math.BigDecimal

internal class KoinosTransactionBuilder(isTestnet: Boolean) {

    private val koinContractAbi = KoinContractAbi(isTestnet = isTestnet)

    @Suppress("MagicNumber")
    suspend fun buildToSign(
        transactionData: TransactionData,
        currentNonce: KoinosAccountNonce,
        koinContractIdHolder: KoinosContractIdHolder,
    ): Result<Pair<KoinosProtocol.Transaction, ByteArray>> {
        transactionData.requireUncompiled()

        val from = transactionData.sourceAddress
        val to = transactionData.destinationAddress
        val amount = transactionData.amount.longValue

        val manaLimit = (transactionData.extras as? KoinosTransactionExtras)?.manaLimit
            ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val manaLimitSatoshi = manaLimit
            .movePointRight(Blockchain.Koinos.decimals())
            .longValueExact()

        val nextNonce = currentNonce.nonce.inc()

        val operation = koinos.protocol.operation(
            call_contract = koinos.protocol.call_contract_operation(
                contract_id = koinContractIdHolder.get()
                    .successOr { return Result.Failure(BlockchainSdkError.FailedToBuildTx) }
                    .decodeBase58()!!
                    .toByteString(),
                entry_point = koinContractAbi.transfer.entryPoint,
                args = koinContractAbi.transfer.argsToProto(
                    fromAccount = from,
                    toAccount = to,
                    value = amount,
                )?.encodeByteString() ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
            ),
        )

        val operationSha256 = operation.encodeByteString().sha256()
        val operationMerkleRoot = ByteString.of(18, 32, *operationSha256.toByteArray())
        val encodedNextNonce = koinos.chain.value_type(uint64_value = nextNonce.toLong()).encodeByteString()

        val header = koinos.protocol.transaction_header(
            chain_id = koinContractAbi.chainId.decodeBase64()!!,
            rc_limit = manaLimitSatoshi,
            nonce = encodedNextNonce,
            operation_merkle_root = operationMerkleRoot,
            payer = koinContractAbi.addressToByteString(from)
                ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx),
        )

        val hashToSign = header.encodeByteString().sha256()
        val transactionId = "0x1220${hashToSign.hex()}"

        val transactionToSign = KoinosProtocol.Transaction(
            header = KoinosProtocol.TransactionHeader(
                chainId = koinContractAbi.chainId,
                rcLimit = manaLimitSatoshi,
                nonce = encodedNextNonce.base64Url(),
                operationMerkleRoot = operationMerkleRoot.base64Url(),
                payer = from,
            ),
            id = transactionId,
            operations = listOf(
                KoinosProtocol.Operation(
                    callContract = KoinosProtocol.CallContractOperation(
                        contractIdBase58 = koinContractIdHolder.get()
                            .successOr { return Result.Failure(BlockchainSdkError.FailedToBuildTx) },
                        entryPoint = koinContractAbi.transfer.entryPoint,
                        argsBase64 = operation.call_contract!!.args.base64Url(),
                    ),
                ),
            ),
            signatures = emptyList(),
        )

        return Result.Success(transactionToSign to hashToSign.toByteArray())
    }

    @Suppress("MagicNumber")
    fun buildToSend(
        transaction: KoinosProtocol.Transaction,
        signature: ByteArray,
        hashToSign: ByteArray,
        publicKey: Wallet.PublicKey,
    ): KoinosProtocol.Transaction {
        val extendedSignature = UnmarshalHelper()
            .unmarshalSignatureExtended(signature = signature, hash = hashToSign, publicKey = publicKey)

        val v = extendedSignature.recId + 31

        val rsSignature = extendedSignature.r.toByteArray().removeLeadingZero() +
            extendedSignature.s.toByteArray().removeLeadingZero()

        val finalSignature = byteArrayOf(v.toByte()) + rsSignature

        return transaction.copy(
            signatures = listOf(finalSignature.toByteString().base64Url()),
        )
    }
}

@JvmInline
value class KoinosTransactionExtras(val manaLimit: BigDecimal) : TransactionExtras