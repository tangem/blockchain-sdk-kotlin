package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import java.math.BigDecimal
import java.math.BigInteger

class BitcoinCashTransactionBuilder(walletPublicKey: ByteArray, private val blockchain: Blockchain) :
    BitcoinTransactionBuilder(walletPublicKey.toCompressedPublicKey(), blockchain) {

    override fun buildToSign(transactionData: TransactionData): Result<List<ByteArray>> {
        if (unspentOutputs.isNullOrEmpty()) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Unspent outputs are missing"),
            )
        }

        val change: BigDecimal = calculateChange(transactionData, unspentOutputs!!)

        transaction = transactionData.toBitcoinCashTransaction(networkParameters, unspentOutputs!!, change, blockchain)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            val value = Coin.parseCoin(unspentOutputs!![index].amount.toString())
            hashesForSign[index] = getTransaction().hashForSignatureWitness(
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

    private fun getTransaction() = transaction as BitcoinCashTransaction
}

internal fun TransactionData.toBitcoinCashTransaction(
    networkParameters: NetworkParameters?,
    unspentOutputs: List<BitcoinUnspentOutput>,
    change: BigDecimal,
    blockchain: Blockchain,
): BitcoinCashTransaction {
    val transaction = BitcoinCashTransaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    val addressService = BitcoinCashAddressService(blockchain)
    val sourceLegacyAddress =
        LegacyAddress.fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(this.sourceAddress))

    val destinationLegacyAddress = if (addressService.validateCashAddrAddress(this.destinationAddress)) {
        LegacyAddress.fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(this.destinationAddress))
    } else {
        LegacyAddress.fromBase58(networkParameters, this.destinationAddress)
    }

    transaction.addOutput(
        Coin.parseCoin(this.amount.value!!.toPlainString()),
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
