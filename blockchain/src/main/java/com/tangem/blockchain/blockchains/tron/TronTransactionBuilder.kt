package com.tangem.blockchain.blockchains.tron

import com.squareup.wire.AnyMessage
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.padLeft
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import org.tron.protos.BlockHeader
import org.tron.protos.Transaction
import protocol.TransferContract
import protocol.TriggerSmartContract

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
            is AmountType.Token -> buildContractForToken(amount, source, destination, extras)
            else -> error("Not supported")
        }
        val feeLimit = if (amount.type == AmountType.Coin) 0L else SMART_CONTRACT_FEE_LIMIT

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

    fun buildForSend(rawData: Transaction.raw, signature: ByteArray): Transaction {
        return Transaction(rawData, listOf(signature.toByteString()))
    }

    private fun buildContractForCoin(amount: Amount, source: String, destination: String): Transaction.Contract {
        val parameter = TransferContract(
            owner_address = source.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
            to_address = destination.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
            amount = amount.longValue ?: 0L,
        )
        return Transaction.Contract(
            type = Transaction.Contract.ContractType.TransferContract,
            parameter = AnyMessage.pack(parameter),
        )
    }

    private fun buildContractForToken(
        amount: Amount,
        source: String,
        destination: String,
        extras: TronTransactionExtras?,
    ): Transaction.Contract {
        return if (extras == null) {
            buildContractForTransferToken(amount, source, destination)
        } else {
            when (extras.txType) {
                TransactionType.APPROVE -> buildContractForApproveToken(
                    data = extras.data,
                    sourceAddress = source,
                    destination = destination,
                )
            }
        }
    }

    @Suppress("MagicNumber")
    private fun buildContractForTransferToken(
        amount: Amount,
        source: String,
        destination: String,
    ): Transaction.Contract {
        val amountType = amount.type as? AmountType.Token ?: error("wrong amount type")
        val functionSelector = "transfer(address,uint256)"
        val functionSelectorHash =
            functionSelector.toByteArray().toKeccak().slice(0 until 4).toByteArray()

        val addressData = destination
            .decodeBase58(checked = true)
            ?.padLeft(32)
            ?: byteArrayOf()

        val amountData = amount
            .bigIntegerValue()
            ?.toByteArray()
            ?.padLeft(32)
            ?: byteArrayOf()

        val contractData = functionSelectorHash + addressData + amountData

        val parameter = TriggerSmartContract(
            contract_address = amountType.token.contractAddress
                .decodeBase58(true)?.toByteString() ?: EMPTY,
            data_ = contractData.toByteString(),
            owner_address = source.decodeBase58(true)?.toByteString() ?: EMPTY,
        )

        return Transaction.Contract(
            type = Transaction.Contract.ContractType.TriggerSmartContract,
            parameter = AnyMessage.pack(parameter),
        )
    }

    @Suppress("MagicNumber")
    private fun buildContractForApproveToken(
        data: ByteArray,
        sourceAddress: String,
        destination: String,
    ): Transaction.Contract {
        val functionSelector = "approve(address,uint256)"
        val functionSelectorHash =
            functionSelector.toByteArray().toKeccak().slice(0 until 4).toByteArray()

        val contractData = functionSelectorHash + data

        val parameter = TriggerSmartContract(
            contract_address = destination.decodeBase58(true)?.toByteString() ?: EMPTY,
            data_ = contractData.toByteString(),
            owner_address = sourceAddress.decodeBase58(true)?.toByteString() ?: EMPTY,
        )

        return Transaction.Contract(
            type = Transaction.Contract.ContractType.TriggerSmartContract,
            parameter = AnyMessage.pack(parameter),
        )
    }

    @Suppress("MagicNumber")
    private fun contract(amount: Amount, source: String, destination: String): Transaction.Contract {
        return when (amount.type) {
            AmountType.Coin -> {
                val parameter = TransferContract(
                    owner_address = source.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
                    to_address = destination.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
                    amount = amount.longValue ?: 0L,
                )
                Transaction.Contract(
                    type = Transaction.Contract.ContractType.TransferContract,
                    parameter = AnyMessage.pack(parameter),
                )
            }
            is AmountType.Token -> {
                val functionSelector = "transfer(address,uint256)"
                val functionSelectorHash =
                    functionSelector.toByteArray().toKeccak().slice(0 until 4).toByteArray()

                val addressData = destination
                    .decodeBase58(checked = true)
                    ?.padLeft(32)
                    ?: byteArrayOf()

                val amountData = amount
                    .bigIntegerValue()
                    ?.toByteArray()
                    ?.padLeft(32)
                    ?: byteArrayOf()

                val contractData = functionSelectorHash + addressData + amountData

                val parameter = TriggerSmartContract(
                    contract_address = amount.type.token.contractAddress
                        .decodeBase58(true)?.toByteString() ?: EMPTY,
                    data_ = contractData.toByteString(),
                    owner_address = source.decodeBase58(true)?.toByteString() ?: EMPTY,
                )

                Transaction.Contract(
                    type = Transaction.Contract.ContractType.TriggerSmartContract,
                    parameter = AnyMessage.pack(parameter),
                )
            }
            else -> error("Not supported")
        }
    }

    companion object {
        const val SMART_CONTRACT_FEE_LIMIT = 100_000_000L
    }
}