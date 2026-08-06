package com.tangem.blockchain.blockchains.tron

import com.squareup.moshi.Moshi
import com.squareup.wire.AnyMessage
import com.tangem.blockchain.blockchains.tron.gasless.TronWebContract
import com.tangem.blockchain.blockchains.tron.gasless.TronWebParameter
import com.tangem.blockchain.blockchains.tron.gasless.TronWebRawData
import com.tangem.blockchain.blockchains.tron.gasless.TronWebSignedTransaction
import com.tangem.blockchain.blockchains.tron.gasless.TronWebTriggerValue
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toHexString
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

        return buildRaw(block = block, contract = contract, feeLimit = feeLimit, memo = extras?.memo)
    }

    /**
     * Builds the transaction to sign from a [TransactionData], mirroring the EVM
     * `buildForSign(TransactionData)` shape and dispatching by amount type:
     * - native coin transfer (Coin, no extras or extras without call data) → `TransferContract`;
     * - TRC-20 token transfer (Token) → `TriggerSmartContract` on the token contract;
     * - native-value contract call (Coin + call data, e.g. a DEX swap in EVM format) →
     *   `TriggerSmartContract` on the destination router with `call_value` + `data`.
     *
     * Any [TronTransactionExtras.memo] is attached to the resulting transaction regardless of shape.
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
        val swapCallData = extras.callDataOrNull()

        val contract = when (amount.type) {
            AmountType.Coin -> if (swapCallData != null) {
                buildContractForSmartContractCall(
                    amount = amount,
                    source = source,
                    destination = destination,
                    callData = swapCallData,
                )
            } else {
                buildContractForCoin(amount, source, destination)
            }
            is AmountType.Token -> buildContractForToken(amount, source, extras)
            else -> error("Not supported")
        }
        val feeLimit = if (amount.type == AmountType.Coin && swapCallData == null) 0L else SMART_CONTRACT_FEE_LIMIT

        return buildRaw(block = block, contract = contract, feeLimit = feeLimit, memo = extras?.memo)
    }

    fun buildForSend(rawData: Transaction.raw, signature: ByteArray): Transaction {
        return Transaction(rawData, listOf(signature.toByteString()))
    }

    private fun buildContractForToken(
        amount: Amount,
        source: String,
        extras: TronTransactionExtras?,
    ): Transaction.Contract {
        val callData = extras?.callData ?: error("Smart contract is not specified")
        val amountType = amount.type as? AmountType.Token ?: error("wrong amount type")

        return buildTriggerSmartContract(
            ownerAddress = source,
            contractAddress = amountType.token.contractAddress,
            data = callData.data,
        )
    }

    private fun buildContractForSmartContractCall(
        amount: Amount,
        source: String,
        destination: String,
        callData: SmartContractCallData,
    ): Transaction.Contract = buildTriggerSmartContract(
        ownerAddress = source,
        contractAddress = destination,
        data = callData.data,
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
     * wraps the already-built [contract] with the given [feeLimit] and optional [memo].
     *
     * [memo] lands in the transaction-level `data` field. A `null` memo encodes identically to the
     * proto3 default, so transactions without one are byte-for-byte unchanged.
     */
    @Suppress("MagicNumber")
    private fun buildRaw(
        block: TronBlock,
        contract: Transaction.Contract,
        feeLimit: Long,
        memo: ByteArray? = null,
    ): Transaction.raw {
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
            data_ = memo?.toByteString() ?: EMPTY,
        )
    }

    /**
     * Serialises a signed Tron transaction into the TronWeb-compatible JSON format that the
     * gasless backend expects (`JSON.parse`-able signed transaction object).
     *
     * Only [Transaction.Contract.ContractType.TriggerSmartContract] contracts are supported
     * because gasless operations are limited to token transfers via smart contracts.
     */
    fun buildSignedTronWebJson(rawData: Transaction.raw, signature: ByteArray): String {
        val contract = rawData.contract.firstOrNull() ?: error("Tron raw_data has no contract")
        require(contract.type == Transaction.Contract.ContractType.TriggerSmartContract) {
            "Only TriggerSmartContract is supported for gasless, got ${contract.type}"
        }
        val trigger = contract.parameter?.unpack(TriggerSmartContract.ADAPTER)
            ?: error("Failed to unpack TriggerSmartContract")

        val rawBytes = rawData.encode()
        val dto = TronWebSignedTransaction(
            isVisible = false,
            txId = rawBytes.calculateSha256().toHexString().lowercase(),
            rawDataHex = rawBytes.toHexString().lowercase(),
            signature = listOf(signature.toHexString().lowercase()),
            rawData = TronWebRawData(
                contract = listOf(
                    TronWebContract(
                        type = "TriggerSmartContract",
                        parameter = TronWebParameter(
                            typeUrl = "type.googleapis.com/protocol.TriggerSmartContract",
                            value = TronWebTriggerValue(
                                data = trigger.data_.toByteArray().toHexString().lowercase(),
                                ownerAddress = trigger.owner_address.toByteArray().toHexString().lowercase(),
                                contractAddress = trigger.contract_address.toByteArray().toHexString().lowercase(),
                            ),
                        ),
                    ),
                ),
                refBlockBytes = rawData.ref_block_bytes.toByteArray().toHexString().lowercase(),
                refBlockHash = rawData.ref_block_hash.toByteArray().toHexString().lowercase(),
                expiration = rawData.expiration,
                feeLimit = rawData.fee_limit.takeIf { it > 0 },
                timestamp = rawData.timestamp,
            ),
        )
        return tronWebAdapter.toJson(dto)
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

        private val tronWebAdapter by lazy {
            Moshi.Builder().build().adapter(TronWebSignedTransaction::class.java)
        }
    }
}