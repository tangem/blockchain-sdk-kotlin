package com.tangem.blockchain.blockchains.aptos.network.converter

import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.blockchains.aptos.network.request.AptosTransactionBody

/**
 * Converter from [AptosTransactionInfo] to [AptosTransactionBody]
 *
 * @author Andrew Khokhlov on 16/01/2024
 */
internal object AptosPseudoTransactionConverter {

    private const val TRANSFER_PAYLOAD_TYPE = "entry_function_payload"
    private const val TRANSFER_PAYLOAD_FUNCTION = "0x1::aptos_account::transfer"
    private const val SIGNATURE_TYPE = "ed25519_signature"

    /** Max gas amount doesn't matter for fee calculating */
    private const val PSEUDO_TRANSACTION_MAX_GAS_AMOUNT = 100_000L

    /** Transaction hash doesn't matter for fee calculating */
    private const val PSEUDO_TRANSACTION_HASH = "0x00000000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000000"

    fun convert(from: AptosTransactionInfo): AptosTransactionBody {
        return AptosTransactionBody(
            sender = from.sourceAddress,
            sequenceNumber = from.sequenceNumber.toString(),
            expirationTimestamp = from.expirationTimestamp.toString(),
            gasUnitPrice = from.gasUnitPrice.toString(),
            maxGasAmount = PSEUDO_TRANSACTION_MAX_GAS_AMOUNT.toString(),
            payload = createTransferPayload(from.destinationAddress, from.amount),
            signature = createSignature(publicKey = from.publicKey),
        )
    }

    private fun createTransferPayload(destination: String, amount: Long): AptosTransactionBody.Payload {
        return AptosTransactionBody.Payload(
            type = TRANSFER_PAYLOAD_TYPE,
            function = TRANSFER_PAYLOAD_FUNCTION,
            argumentTypes = listOf(),
            arguments = listOf(destination, amount.toString()),
        )
    }

    private fun createSignature(publicKey: String): AptosTransactionBody.Signature {
        return AptosTransactionBody.Signature(
            type = SIGNATURE_TYPE,
            publicKey = publicKey,
            signature = PSEUDO_TRANSACTION_HASH,
        )
    }
}
