package com.tangem.blockchain.blockchains.tron

import com.squareup.wire.AnyMessage
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import org.tron.protos.BlockHeader
import org.tron.protos.Transaction
import org.tron.protos.contract.TransferContract
import org.tron.protos.contract.TriggerSmartContract

class TronTransactionBuilder {

    @Suppress("MagicNumber")
    fun buildForSign(
        amount: Amount,
        source: String,
        destination: String,
        block: TronBlock,
        extras: TronTransactionExtras?,
    ): Transaction.raw {
        val contract = when (amount.type) {
            AmountType.Coin -> buildContractForCoin(amount, source, destination)
            is AmountType.Token -> buildContractForToken(amount, source, extras)
            else -> error("Not supported")
        }
        val feeLimit = if (amount.type == AmountType.Coin) 0L else SMART_CONTRACT_FEE_LIMIT

        return buildRaw(block, contract, feeLimit)
    }

    /**
     * [REDACTED_TASK_KEY]: builds the transaction to sign from a [TransactionData], mirroring the EVM
     * `buildForSign(TransactionData)` shape and dispatching by amount type:
     * - native coin transfer (Coin, no extras) → `TransferContract`;
     * - TRC-20 token transfer (Token) → `TriggerSmartContract` on the token contract;
     * - native-value contract call (Coin + call data, e.g. a DEX swap in EVM format) →
     *   `TriggerSmartContract` on the destination router with `call_value` + `data`.
     *
     * Shares the raw-tx assembly ([buildRaw]) with the decomposed overload above; the two differ only
     * in how the contract is resolved and in the fee limit. The decomposed overload is kept so the
     * existing send/CEX call sites stay untouched.
     */
    fun buildForSign(transaction: TransactionData.Uncompiled, block: TronBlock): Transaction.raw {
        val amount = transaction.amount
        val source = transaction.sourceAddress
        val destination = transaction.destinationAddress
        val extras = transaction.extras as? TronTransactionExtras

        val contract = when (amount.type) {
            AmountType.Coin -> if (extras != null) {
                buildContractForSmartContractCall(
                    amount = amount,
                    source = source,
                    destination = destination,
                    extras = extras,
                )
            } else {
                buildContractForCoin(amount, source, destination)
            }
            is AmountType.Token -> buildContractForToken(amount, source, extras)
            else -> error("Not supported")
        }
        val feeLimit = if (amount.type == AmountType.Coin && extras == null) 0L else SMART_CONTRACT_FEE_LIMIT

        return buildRaw(block, contract, feeLimit)
    }

    fun buildForSend(rawData: Transaction.raw, signature: ByteArray): Transaction {
        return Transaction(rawData, listOf(signature.toByteString()))
    }

    private fun buildContractForToken(
        amount: Amount,
        source: String,
        extras: TronTransactionExtras?,
    ): Transaction.Contract {
        if (extras == null) error("Smart contract is not specified")
        val amountType = amount.type as? AmountType.Token ?: error("wrong amount type")

        return buildTriggerSmartContract(
            ownerAddress = source,
            contractAddress = amountType.token.contractAddress,
            data = extras.callData.data,
        )
    }

    private fun buildContractForSmartContractCall(
        amount: Amount,
        source: String,
        destination: String,
        extras: TronTransactionExtras,
    ): Transaction.Contract = buildTriggerSmartContract(
        ownerAddress = source,
        contractAddress = destination,
        data = extras.callData.data,
        callValue = amount.longValue,
    )

    /**
     * Canonical builder for a `TriggerSmartContract` call wrapped in a [Transaction.Contract]: sets the
     * owner, the target [contractAddress], the raw call [data] and an optional native [callValue].
     *
     * [callValue] defaults to `0` — a TRC-20 transfer sends no native TRX (the amount lives inside
     * [data]); a native-value contract call (DEX swap) passes the TRX amount here.
     */
    private fun buildTriggerSmartContract(
        ownerAddress: String,
        contractAddress: String,
        data: ByteArray,
        callValue: Long = 0,
    ): Transaction.Contract {
        val parameter = TriggerSmartContract(
            owner_address = ownerAddress.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
            contract_address = contractAddress.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
            call_value = callValue,
            data_ = data.toByteString(),
        )
        return Transaction.Contract(
            type = Transaction.Contract.ContractType.TriggerSmartContract,
            parameter = AnyMessage.pack(parameter),
        )
    }

    /**
     * Assembles the signable [Transaction.raw] shared by both `buildForSign` overloads: derives the
     * `ref_block_hash` / `ref_block_bytes` and the expiration from the reference [block] header, then
     * wraps the already-built [contract] with the given [feeLimit].
     */
    @Suppress("MagicNumber")
    private fun buildRaw(block: TronBlock, contract: Transaction.Contract, feeLimit: Long): Transaction.raw {
        val blockHeaderRawData = block.blockHeader.rawData
        val blockHeader = BlockHeader.raw(
            timestamp = blockHeaderRawData.timestamp,
            number = blockHeaderRawData.number,
            version = blockHeaderRawData.version,
            txTrieRoot = blockHeaderRawData.txTrieRoot.hexToBytes().toByteString(),
            parentHash = blockHeaderRawData.parentHash.hexToBytes().toByteString(),
            witness_address = blockHeaderRawData.witnessAddress.hexToBytes().toByteString(),
        )

        val blockHash = blockHeader.encode().calculateSha256()
        val refBlockHash = blockHash.slice(8 until 16)
            .toByteArray()
            .toByteString()
        val number = blockHeader.number
        val numberData = number.toByteArray()
        val refBlockBytes = numberData.slice(6 until 8)
            .toByteArray()
            .toByteString()

        val tenHours = 10 * 60 * 60 * 1000

        return Transaction.raw(
            timestamp = blockHeader.timestamp,
            expiration = blockHeader.timestamp + tenHours,
            ref_block_hash = refBlockHash,
            ref_block_bytes = refBlockBytes,
            contract = listOf(contract),
            fee_limit = feeLimit,
        )
    }

    private fun buildContractForCoin(amount: Amount, source: String, destination: String): Transaction.Contract {
        val parameter = TransferContract(
            owner_address = source.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
            to_address = destination.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
            amount = amount.longValue,
        )
        return Transaction.Contract(
            type = Transaction.Contract.ContractType.TransferContract,
            parameter = AnyMessage.pack(parameter),
        )
    }

    companion object {
        const val SMART_CONTRACT_FEE_LIMIT = 100_000_000L
    }
}