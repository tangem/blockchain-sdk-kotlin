package com.tangem.blockchain.blockchains.aptos.network.converter

import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.blockchains.aptos.network.request.AptosTransactionBody

/**
 * Converter from [AptosTransactionInfo] to [AptosTransactionBody]
 *
 * @author Andrew Khokhlov on 16/01/2024
 */
internal object AptosTransactionConverter {

    private const val TRANSFER_PAYLOAD_TYPE = "entry_function_payload"
    private const val TRANSFER_PAYLOAD_FUNCTION = "0x1::aptos_account::transfer_coins"
    private const val APTOS_COIN_CONTRACT = "0x1::aptos_coin::AptosCoin"
    private const val SIGNATURE_TYPE = "ed25519_signature"

    fun convert(from: AptosTransactionInfo): AptosTransactionBody {
        return AptosTransactionBody(
            sender = from.sourceAddress,
            sequenceNumber = from.sequenceNumber.toString(),
            expirationTimestamp = from.expirationTimestamp.toString(),
            gasUnitPrice = from.gasUnitPrice.toString(),
            maxGasAmount = from.maxGasAmount.toString(),
            payload = createTransferPayload(from.destinationAddress, from.amount, from.contractAddress),
            signature = from.hash?.let { createSignature(publicKey = from.publicKey, hash = it) },
        )
    }

    private fun createTransferPayload(
        destination: String,
        amount: Long,
        contractAddress: String?,
    ): AptosTransactionBody.Payload {
        return AptosTransactionBody.Payload(
            type = TRANSFER_PAYLOAD_TYPE,
            function = TRANSFER_PAYLOAD_FUNCTION,
            argumentTypes = listOf(contractAddress ?: APTOS_COIN_CONTRACT),
            arguments = listOf(destination, amount.toString()),
        )
    }

    private fun createSignature(publicKey: String, hash: String): AptosTransactionBody.Signature {
        return AptosTransactionBody.Signature(
            type = SIGNATURE_TYPE,
            publicKey = publicKey,
            signature = hash,
        )
    }
}