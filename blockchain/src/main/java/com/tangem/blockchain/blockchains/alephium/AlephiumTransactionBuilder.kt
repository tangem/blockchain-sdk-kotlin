package com.tangem.blockchain.blockchains.alephium

import com.tangem.blockchain.blockchains.alephium.network.AlephiumResponse
import com.tangem.blockchain.blockchains.alephium.source.*
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toCompressedPublicKey
import kotlinx.io.bytestring.ByteString
import java.math.BigDecimal

internal class AlephiumTransactionBuilder(
    private val publicKey: ByteArray,
    private val blockchain: Blockchain,
) {
    private val publicKeyByteString = ByteString(publicKey.toCompressedPublicKey())
    private val lockupScript = LockupScript.p2pkh(publicKeyByteString)

    var unspentOutputs: List<AssetOutputInfo>? = null

    fun requestOutputs(): Result<List<AssetOutputInfo>> {
        val unspentOutputs = this.unspentOutputs
        return if (unspentOutputs.isNullOrEmpty()) {
            Result.Failure(BlockchainSdkError.CustomError("Unspent outputs are missing"))
        } else {
            Result.Success(unspentOutputs)
        }
    }

    fun updateUnspentOutputs(utxos: AlephiumResponse.Utxos) {
        val nowMillis = System.currentTimeMillis()
        unspentOutputs = utxos.utxos.filter { it.isNotFromFuture(nowMillis) }.map {
            AssetOutputInfo(
                ref = AssetOutputRef(
                    hint = Hint(it.ref.hint),
                    key = TxOutputRef.Key(Blake2b256(ByteString(it.ref.key.hexToBytes()))),
                ),
                outputType = UnpersistedBlockOutput,
                output = AssetOutput(
                    amount = U256.unsafe(it.amount.toBigDecimal()),
                    lockupScript = lockupScript,
                    lockTime = TimeStamp(it.lockTime),
                    tokens = listOf(),
                    additionalData = ByteString(it.additionalData?.hexToBytes() ?: byteArrayOf()),
                ),
            )
        }
    }

    fun serializeUnsignedTransaction(unsignedTransaction: UnsignedTransaction) =
        UnsignedTransaction.serde.serialize(unsignedTransaction)

    fun buildToSign(transactionData: TransactionData): Result<UnsignedTransaction> {
        val uncompiled = transactionData.requireUncompiled()
        return buildToSign(
            destinationAddress = uncompiled.destinationAddress,
            amount = uncompiled.amount,
            fee = uncompiled.fee as Fee.Alephium,
        )
    }

    fun buildToSign(destinationAddress: String, amount: Amount, fee: Fee.Alephium): Result<UnsignedTransaction> {
        return innerBuildToSign(
            destinationAddress = destinationAddress,
            amount = amount,
            fee = fee,
        )
    }

    private fun innerBuildToSign(
        destinationAddress: String,
        amount: Amount,
        fee: Fee.Alephium,
    ): Result<UnsignedTransaction> {
        val unspentOutputs = getMaxUnspentsToSpend()
        val lockupScript = LockupScript.p2pkh(publicKeyByteString)
        val unlockScript = UnlockScript.P2PKH(publicKeyByteString)
        val innerAmount = U256.unsafe(amount.value?.movePointRight(blockchain.decimals()) ?: BigDecimal.ZERO)
        val decodeAddress = destinationAddress.decodeBase58(false)
            ?: return Result.Failure(BlockchainSdkError.AccountNotFound())
        val decodeAddressWithoutPrefix = ByteString(decodeAddress).substring(1)
        val outputInfos = TxOutputInfo(
            lockupScript = LockupScript.P2PKH(Blake2b256(decodeAddressWithoutPrefix)),
            attoAlphAmount = innerAmount,
            tokens = listOf(),
            lockTime = null,
        )
        val gasPrice = fee.gasPrice
        val gasAmount = fee.gasAmount
        val networkId = if (blockchain == Blockchain.Alephium) NetworkId.mainNet else NetworkId.testNet
        // portal to alephium sources https://github.com/alephium/alephium
        val unsignedTransaction = TxUtils.transfer(
            fromLockupScript = lockupScript,
            fromUnlockScript = unlockScript,
            outputInfos = outputInfos,
            gasOpt = GasBox(fee.gasAmount.toInt()),
            gasPrice = GasPrice(U256.unsafe(fee.gasPrice)),
            utxos = unspentOutputs,
            networkId = networkId,
        ).getOrElse {
            val error = it as? BlockchainSdkError ?: BlockchainSdkError.FailedToBuildTx
            return Result.Failure(error)
        }
        if (unsignedTransaction.inputs.size > MAX_INPUT_COUNT) {
            val maxAmount = getMaxUnspentsToSpendAmount()

            val fee = gasPrice * gasAmount
            return Result.Failure(
                BlockchainSdkError.Alephium.UtxoAmountError(
                    MAX_INPUT_COUNT,
                    (maxAmount - fee).movePointLeft(blockchain.decimals()),
                ),
            )
        }
        return Result.Success(unsignedTransaction)
    }

    fun getMaxUnspentsToSpendCount(): Int {
        val count = unspentOutputs?.size ?: 0
        return if (count < MAX_INPUT_COUNT) count else MAX_INPUT_COUNT
    }

    fun getMaxUnspentsToSpend() = unspentOutputs!!
        .sortedByDescending { it.output.amount.v }
        .take(getMaxUnspentsToSpendCount())

    fun getMaxUnspentsToSpendAmount() = getMaxUnspentsToSpend()
        .sumOf { it.output.amount.v }
        .toBigDecimal()

    companion object {
        const val MAX_INPUT_COUNT = 256 // Alephium rejects transactions with more inputs
    }
}