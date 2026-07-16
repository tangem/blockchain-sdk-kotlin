package com.tangem.blockchain.blockchains.bitcoin.psbt

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.common.extensions.hexToBytes
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxHash
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.Either

/**
 * Shared PSBT fixtures for sighash-strategy and signature-application tests.
 *
 * Built programmatically (no opaque base64 literals) so it's obvious which key/script owns each input.
 */
internal object PsbtTestFixtures {

    /** Compressed public key owning the witness input of [singleP2wpkhInputPsbt]. */
    val ownerPublicKey: ByteArray =
        "0250863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b2352".hexToBytes()

    /** All-0x01 64-byte raw (r||s) signature; passes through [com.tangem.blockchain.extensions.toCanonicalECDSASignature]. */
    val dummySignature64: ByteArray = ByteArray(64) { 0x01 }

    /** Bitcoin mainnet legacy (base58) address for [ownerPublicKey]. */
    val legacyOwnerAddress: String =
        BitcoinAddressService(Blockchain.Bitcoin).makeLegacyAddress(ownerPublicKey).value

    /**
     * A PSBT with exactly one P2WPKH input (witnessUtxo owned by [ownerPublicKey]) and one arbitrary output.
     */
    fun singleP2wpkhInputPsbt(): Psbt {
        val ownerScript = Script.pay2wpkh(Crypto.hash160(ownerPublicKey))
        val witnessUtxo = TxOut(Satoshi(100_000L), ownerScript)

        val outPoint = OutPoint(TxHash(ByteArray(32)), 0L)
        val txIn = TxIn(outPoint, emptyList(), TxIn.SEQUENCE_FINAL)

        val destinationScript = Script.pay2wpkh(ByteArray(20) { 0x11 })
        val txOut = TxOut(Satoshi(90_000L), destinationScript)

        val unsignedTx = Transaction(version = 2L, txIn = listOf(txIn), txOut = listOf(txOut), lockTime = 0L)
        val psbt = Psbt(unsignedTx)

        return when (val result = psbt.updateWitnessInput(outPoint, witnessUtxo)) {
            is Either.Right -> result.value
            is Either.Left -> error("Failed to build singleP2wpkhInputPsbt fixture: ${result.value}")
        }
    }

    /**
     * A PSBT with exactly one legacy P2PKH input (nonWitnessUtxo owned by [ownerPublicKey]) and one arbitrary output.
     */
    fun singleP2pkhLegacyInputPsbt(): Psbt {
        val ownerScript = Script.pay2pkh(Crypto.hash160(ownerPublicKey))

        val prevTxIn = TxIn(OutPoint(TxHash(ByteArray(32)), 0L), emptyList(), TxIn.SEQUENCE_FINAL)
        val prevTxOut = TxOut(Satoshi(100_000L), ownerScript)
        val prevTx = Transaction(version = 2L, txIn = listOf(prevTxIn), txOut = listOf(prevTxOut), lockTime = 0L)
        val vout = 0

        val outPoint = OutPoint(prevTx, vout.toLong())
        val txIn = TxIn(outPoint, emptyList(), TxIn.SEQUENCE_FINAL)

        val destinationScript = Script.pay2pkh(ByteArray(20) { 0x11 })
        val txOut = TxOut(Satoshi(90_000L), destinationScript)

        val unsignedTx = Transaction(version = 2L, txIn = listOf(txIn), txOut = listOf(txOut), lockTime = 0L)
        val psbt = Psbt(unsignedTx)

        return when (val result = psbt.updateNonWitnessInput(prevTx, vout)) {
            is Either.Right -> result.value
            is Either.Left -> error("Failed to build singleP2pkhLegacyInputPsbt fixture: ${result.value}")
        }
    }

    /** Serializes [psbt] to Base64 (no-wrap), matching [com.tangem.blockchain.blockchains.bitcoin.walletconnect.PsbtSerializer.serializePsbt]. */
    fun serialize(psbt: Psbt): String {
        return Psbt.write(psbt).toByteArray().encodeBase64NoWrap()
    }
}