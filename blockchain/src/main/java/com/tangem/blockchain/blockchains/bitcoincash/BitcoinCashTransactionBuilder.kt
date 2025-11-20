package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoincash.cashaddr.BitcoinCashAddressType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.getMinimumRequiredUTXOsToSend
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import java.math.BigDecimal
import java.math.BigInteger

class BitcoinCashTransactionBuilder(walletPublicKey: ByteArray, private val blockchain: Blockchain) :
    BitcoinTransactionBuilder(walletPublicKey.toCompressedPublicKey(), blockchain) {

    override fun buildToSign(transactionData: TransactionData, dustValue: BigDecimal?): Result<List<ByteArray>> {
        val uncompiled = transactionData.requireUncompiled()
        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Unspent outputs are missing"),
            )
        }

        val failResult = Result.Failure(BlockchainSdkError.FailedToBuildTx)

        val outputsToSend = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs ?: return failResult,
            transactionAmount = uncompiled.amount.value ?: return failResult,
            transactionFeeAmount = uncompiled.fee?.amount?.value ?: return failResult,
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
            val value = Coin.parseCoin(outputsToSend[index].amount.toString())
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
        val sigHash = 0x41
        return TransactionSignature(r, canonicalS, sigHash)
    }

    private fun getBitcoinCashTransaction() = transaction as BitcoinCashTransaction
}

internal fun TransactionData.toBitcoinCashTransaction(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<BitcoinUnspentOutput>,
    change: BigDecimal,
    blockchain: Blockchain,
): BitcoinCashTransaction {
    val uncompiled = requireUncompiled()

    val transaction = BitcoinCashTransaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    val addressService = BitcoinCashAddressService(blockchain)
    val sourceLegacyAddress =
        LegacyAddress.fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(uncompiled.sourceAddress))

    val destinationLegacyAddress = if (addressService.validateCashAddrAddress(uncompiled.destinationAddress)) {
        getLegacyAddressFromCashAddr(
            destinationAddress = uncompiled.destinationAddress,
            addressService = addressService,
            networkParameters = networkParameters,
        )
    } else {
        LegacyAddress.fromBase58(networkParameters, uncompiled.destinationAddress)
    }

    transaction.addOutput(
        Coin.parseCoin(uncompiled.amount.value!!.toPlainString()),
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