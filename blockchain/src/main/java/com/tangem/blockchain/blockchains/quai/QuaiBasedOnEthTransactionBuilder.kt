package com.tangem.blockchain.blockchains.quai

import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTWTransactionBuilder
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.Wallet
import org.kethereum.keccakshortcut.keccak
import java.math.BigInteger

internal class QuaiBasedOnEthTransactionBuilder(wallet: Wallet) : EthereumTWTransactionBuilder(wallet) {

    override fun buildForSign(transaction: TransactionData): EthereumCompiledTxInfo.TWInfo {
        val input = buildSigningInput(transaction)
        val unsignedProto = QuaiProtobufUtils.buildUnsignedProto(input)
        val hash = unsignedProto.keccak()
        return EthereumCompiledTxInfo.TWInfo(hash = hash, input = input)
    }

    override fun buildForSend(
        transaction: TransactionData,
        signature: ByteArray,
        compiledTransaction: EthereumCompiledTxInfo,
    ): ByteArray {
        val ext = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = compiledTransaction.hash,
            publicKey = decompressedPublicKey,
        )
        val vBytes = byteArrayOf(ext.recId.toByte())
        val rBytes = toFixedLength32(ext.r)
        val sBytes = toFixedLength32(ext.s)
        return QuaiProtobufUtils.convertSigningInputToProtobuf(
            signingInput = (compiledTransaction as EthereumCompiledTxInfo.TWInfo).input,
            vSignature = vBytes,
            rSignature32 = rBytes,
            sSignature32 = sBytes,
        )
    }

    @Suppress("MagicNumber")
    private fun toFixedLength32(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        val unsigned = if (raw.isNotEmpty() && raw[0].toInt() == 0) raw.copyOfRange(1, raw.size) else raw
        return if (unsigned.size >= 32) {
            unsigned.copyOfRange(unsigned.size - 32, unsigned.size)
        } else {
            ByteArray(32 - unsigned.size) + unsigned
        }
    }
}