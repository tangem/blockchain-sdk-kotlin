package com.tangem.blockchain.blockchains.tron

import com.google.common.primitives.Ints.max
import com.squareup.wire.AnyMessage
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.Companion.toKeccak
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toDecompressedPublicKey
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.removeLeadingZero
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import org.tron.protos.BlockHeader
import org.tron.protos.Transaction
import protocol.TransferContract
import protocol.TriggerSmartContract
import java.math.BigInteger

class TronTransactionBuilder(private val blockchain: Blockchain) {

    fun buildForSign(
        amount: Amount,
        source: String,
        destination: String,
        block: TronBlock
    ): Transaction.raw {
        val contract = contract(amount, source, destination)
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
            fee_limit = feeLimit
        )
    }

    fun buildForSend(rawData: Transaction.raw, signature: ByteArray): Transaction {
        return Transaction(rawData, listOf(signature.toByteString()))
    }

    fun unmarshalSignature(
        signature: ByteArray,
        hash: ByteArray,
        publicKey: Wallet.PublicKey
    ): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            hash,
            PublicKey(publicKey.blockchainKey.toDecompressedPublicKey().sliceArray(1..64))
        )
        val v = (recId + 27).toBigInteger()
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return signatureData.r.toByteArray().removeLeadingZero() +
                signatureData.s.toByteArray().removeLeadingZero() +
                signatureData.v.toByteArray().removeLeadingZero()
    }

    private fun contract(
        amount: Amount,
        source: String,
        destination: String,
    ): Transaction.Contract {

        return when (amount.type) {
            AmountType.Coin -> {
                val parameter = TransferContract(
                    owner_address = source.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
                    to_address = destination.decodeBase58(checked = true)?.toByteString() ?: EMPTY,
                    amount = amount.longValue ?: 0L
                )
                Transaction.Contract(
                    type = Transaction.Contract.ContractType.TransferContract,
                    parameter = AnyMessage.pack(parameter)
                )
            }
            is AmountType.Token -> {
                val functionSelector = "transfer(address,uint256)"
                val functionSelectorHash =
                    functionSelector.toByteArray().toKeccak().slice(0 until 4).toByteArray()

                val addressData = destination.decodeBase58(checked = true)?.padLeft(32)
                    ?: byteArrayOf()

                val amountData = amount.bigIntegerValue()?.toByteArray()
                    ?.padLeft(32)
                    ?: byteArrayOf()

                val contractData = functionSelectorHash + addressData + amountData

                val parameter = TriggerSmartContract(
                    contract_address = amount.type.token.contractAddress
                        .decodeBase58(true)?.toByteString() ?: EMPTY,
                    data_ = contractData.toByteString(),
                    owner_address = source.decodeBase58(true)?.toByteString() ?: EMPTY
                )

                Transaction.Contract(
                    type = Transaction.Contract.ContractType.TriggerSmartContract,
                    parameter = AnyMessage.pack(parameter)
                )
            }
            AmountType.Reserve -> throw Exception("Not supported")
        }
    }

    private fun ByteArray.padLeft(length: Int): ByteArray {
        val paddingSize = max(length - this.size, 0)
        return ByteArray(paddingSize) + this
    }

    companion object {
        const val SMART_CONTRACT_FEE_LIMIT = 100_000_000L
    }

}
