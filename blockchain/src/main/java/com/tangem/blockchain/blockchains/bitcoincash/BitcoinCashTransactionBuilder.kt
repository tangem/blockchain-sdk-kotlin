package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.MultiAddressSignatureInfo
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.getMinimumRequiredUTXOsToSend
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.crypto.hdWallet.DerivationPath
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.math.BigInteger

class BitcoinCashTransactionBuilder(walletPublicKey: ByteArray, private val blockchain: Blockchain) :
    BitcoinTransactionBuilder(walletPublicKey.toCompressedPublicKey(), blockchain) {

    override fun buildToSign(transactionData: TransactionData, dustValue: BigDecimal?): Result<List<ByteArray>> {
        val uncompiledTransaction = transactionData.requireUncompiled()
        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Unspent outputs are missing"),
            )
        }

        val failResult = Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val outputsToSend = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs ?: return failResult,
            transactionAmount = uncompiledTransaction.amount.value ?: return failResult,
            transactionFeeAmount = uncompiledTransaction.fee?.amount?.value ?: return failResult,
            unspentToAmount = { it.amount },
            dustValue = dustValue,
        ).successOr { failure ->
            return failure
        }

        val change: BigDecimal = calculateChange(transactionData, outputsToSend)

        transaction = transactionData.toBitcoinCashTransaction(networkParameters, outputsToSend, change, blockchain)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            val value = Coin.parseCoin(outputsToSend[index].amount.toPlainString())
            hashesForSign[index] = getBitcoinCashTransaction().hashForSignatureWitness(
                index,
                input.scriptBytes,
                value,
                Transaction.SigHash.ALL,
                false,
            ).bytes
        }
        return Result.Success(hashesForSign)
    }

    @Suppress("MagicNumber")
    override fun extractSignature(index: Int, signatures: ByteArray): TransactionSignature {
        val r = BigInteger(1, signatures.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signatures.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val sigHash = BCH_SIGHASH_ALL_FORKID
        return TransactionSignature(r, canonicalS, sigHash)
    }

    /**
     * Build per-input hashes for Dynamic Addresses (multi-derivation) BCH sends.
     *
     * Pairs with [buildToSendMultiAddress]: that call MUST follow the matching
     * [buildToSignMultiAddress] on the SAME builder instance — both methods read/write the
     * shared [transaction] field. Interleaving with [buildToSign] / [buildToSend] (single-address)
     * between the pair will overwrite [transaction] and corrupt the send.
     */
    override fun buildToSignMultiAddress(
        transactionData: TransactionData,
        dustValue: BigDecimal?,
        changeAddress: String?,
    ): Result<List<MultiAddressSignatureInfo>> {
        val uncompiledTransaction = transactionData.requireUncompiled()
        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(BlockchainSdkError.CustomError("Unspent outputs are missing"))
        }
        val failResult = Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val outputsToSend = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs ?: return failResult,
            transactionAmount = uncompiledTransaction.amount.value ?: return failResult,
            transactionFeeAmount = uncompiledTransaction.fee?.amount?.value ?: return failResult,
            unspentToAmount = { it.amount },
            dustValue = dustValue,
        ).successOr { failure -> return failure }

        val change: BigDecimal = calculateChange(transactionData, outputsToSend)
        transaction = transactionData.toBitcoinCashTransactionMultiAddress(
            networkParameters = networkParameters,
            unspentOutputs = outputsToSend,
            change = change,
            blockchain = blockchain,
            changeAddress = changeAddress,
        )

        val signatureInfos = MutableList<MultiAddressSignatureInfo?>(transaction.inputs.size) { null }
        for (input in transaction.inputs) {
            val index = input.index
            val utxo = outputsToSend[index]
            val inputPublicKey = utxo.publicKey
                ?: return Result.Failure(BlockchainSdkError.CustomError("UTXO at index $index has no publicKey"))
            val pathString = utxo.derivationPath
                ?: return Result.Failure(BlockchainSdkError.CustomError("UTXO at index $index has no derivationPath"))
            val derivationPath = runCatching { DerivationPath(pathString) }.getOrElse { e ->
                return Result.Failure(
                    BlockchainSdkError.CustomError("UTXO at index $index has malformed derivationPath: ${e.message}"),
                )
            }

            val value = Coin.parseCoin(utxo.amount.toPlainString())
            val hash = getBitcoinCashTransaction().hashForSignatureWitness(
                index,
                input.scriptBytes,
                value,
                Transaction.SigHash.ALL,
                false,
            ).bytes

            signatureInfos[index] = MultiAddressSignatureInfo(
                hash = hash,
                publicKey = inputPublicKey,
                derivationPath = derivationPath,
            )
        }

        return Result.Success(signatureInfos.filterNotNull())
    }

    /**
     * Serialize a multi-address BCH transaction. MUST be called after a successful
     * [buildToSignMultiAddress] on the same builder instance — it consumes the [transaction] field
     * populated by that call.
     *
     * @param signatures one per input, each exactly 64 bytes (raw r||s, no DER, no sighash byte).
     */
    @Suppress("MagicNumber")
    override fun buildToSendMultiAddress(
        signatures: List<ByteArray>,
        signatureInfos: List<MultiAddressSignatureInfo>,
    ): ByteArray {
        require(signatures.size == transaction.inputs.size) {
            "Expected ${transaction.inputs.size} signatures, got ${signatures.size}"
        }
        for (index in transaction.inputs.indices) {
            val signatureBytes = signatures[index]
            require(signatureBytes.size == RAW_SIGNATURE_BYTES) {
                "Signature at index $index has size ${signatureBytes.size}, expected $RAW_SIGNATURE_BYTES"
            }
            val r = BigInteger(1, signatureBytes.copyOfRange(0, 32))
            val s = BigInteger(1, signatureBytes.copyOfRange(32, 64))
            val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
            val signature = TransactionSignature(r, canonicalS, BCH_SIGHASH_ALL_FORKID)

            val inputPublicKey = signatureInfos[index].publicKey
            transaction.inputs[index].scriptSig = ScriptBuilder.createInputScript(
                signature,
                ECKey.fromPublicOnly(inputPublicKey.toCompressedPublicKey()),
            )
        }
        return transaction.bitcoinSerialize()
    }

    private fun getBitcoinCashTransaction() = transaction as BitcoinCashTransaction

    private companion object {
        const val BCH_SIGHASH_ALL_FORKID = 0x41 // SIGHASH_ALL (0x01) | SIGHASH_FORKID (0x40)
        const val RAW_SIGNATURE_BYTES = 64 // 32-byte r || 32-byte s, before DER encoding
    }
}

internal fun TransactionData.toBitcoinCashTransaction(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<BitcoinUnspentOutput>,
    change: BigDecimal,
    blockchain: Blockchain,
): BitcoinCashTransaction {
    val uncompiledTransaction = requireUncompiled()

    val transaction = BitcoinCashTransaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    val addressService = BitcoinCashAddressService(blockchain)
    val sourceLegacyAddress =
        LegacyAddress.fromPubKeyHash(
            networkParameters,
            addressService.getPublicKeyHash(uncompiledTransaction.sourceAddress),
        )

    val isCashAddrAddress = addressService.validateCashAddrAddress(uncompiledTransaction.destinationAddress)
    val destinationLegacyAddress = if (isCashAddrAddress) {
        getLegacyAddressFromCashAddr(
            destinationAddress = uncompiledTransaction.destinationAddress,
            addressService = addressService,
            networkParameters = networkParameters,
        )
    } else {
        LegacyAddress.fromBase58(networkParameters, uncompiledTransaction.destinationAddress)
    }

    transaction.addOutput(
        Coin.parseCoin(requireNotNull(uncompiledTransaction.amount.value).toPlainString()),
        destinationLegacyAddress,
    )
    if (!change.isZero()) {
        transaction.addOutput(
            Coin.parseCoin(change.toPlainString()),
            sourceLegacyAddress,
        )
    }
    return transaction
}

internal fun TransactionData.toBitcoinCashTransactionMultiAddress(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<BitcoinUnspentOutput>,
    change: BigDecimal,
    blockchain: Blockchain,
    changeAddress: String?,
): BitcoinCashTransaction {
    val uncompiledTransaction = requireUncompiled()

    val transaction = BitcoinCashTransaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    val addressService = BitcoinCashAddressService(blockchain)

    val destinationLegacyAddress = resolveBitcoinCashLegacyAddress(
        addressString = uncompiledTransaction.destinationAddress,
        addressService = addressService,
        networkParameters = networkParameters,
    )
    transaction.addOutput(
        Coin.parseCoin(requireNotNull(uncompiledTransaction.amount.value).toPlainString()),
        destinationLegacyAddress,
    )

    if (!change.isZero()) {
        val resolvedChangeAddressString = changeAddress ?: uncompiledTransaction.sourceAddress
        val changeLegacyAddress = resolveBitcoinCashLegacyAddress(
            addressString = resolvedChangeAddressString,
            addressService = addressService,
            networkParameters = networkParameters,
        )
        transaction.addOutput(
            Coin.parseCoin(change.toPlainString()),
            changeLegacyAddress,
        )
    }
    return transaction
}

private fun resolveBitcoinCashLegacyAddress(
    addressString: String,
    addressService: BitcoinCashAddressService,
    networkParameters: NetworkParameters?,
): LegacyAddress {
    return if (addressService.validateCashAddrAddress(addressString)) {
        getLegacyAddressFromCashAddr(
            destinationAddress = addressString,
            addressService = addressService,
            networkParameters = networkParameters,
        )
    } else {
        LegacyAddress.fromBase58(networkParameters, addressString)
    }
}

private fun getLegacyAddressFromCashAddr(
    destinationAddress: String,
    addressService: BitcoinCashAddressService,
    networkParameters: NetworkParameters?,
): LegacyAddress {
    val addressType = addressService.decodeCashAddrAddress(destinationAddress)?.addressType
    return when (addressType) {
        BitcoinCashAddressType.P2SH -> {
            LegacyAddress.fromScriptHash(
                networkParameters,
                addressService.getPublicKeyHash(destinationAddress),
            )
        }
        BitcoinCashAddressType.P2PKH -> {
            LegacyAddress.fromPubKeyHash(
                networkParameters,
                addressService.getPublicKeyHash(destinationAddress),
            )
        }
        else -> error("Failed to decode CashAddr address")
    }
}