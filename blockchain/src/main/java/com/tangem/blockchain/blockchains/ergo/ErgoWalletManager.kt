package com.tangem.blockchain.blockchains.ergo

import android.util.Log
import com.tangem.blockchain.blockchains.ergo.ErgoConstants.Companion.feeMainNetAddress
import com.tangem.blockchain.blockchains.ergo.models.Address
import com.tangem.blockchain.blockchains.ergo.models.ErgoBox
import com.tangem.blockchain.blockchains.ergo.models.Transaction
import com.tangem.blockchain.blockchains.ergo.models.toErgoBox
import com.tangem.blockchain.blockchains.ergo.models.toHash
import com.tangem.blockchain.blockchains.ergo.models.toInput
import com.tangem.blockchain.blockchains.ergo.network.ErgoAddressResponse
import com.tangem.blockchain.blockchains.ergo.network.api.ErgoApiNetworkProvider
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoAddressRequestData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.CompletionResult
import java.math.BigDecimal

class ErgoWalletManager(
    wallet: Wallet,
    private val networkProvider: ErgoApiNetworkProvider,
) : WalletManager(wallet), TransactionSender {

    override val dustValue: BigDecimal = BigDecimal.ONE
    private val blockchain = wallet.blockchain

    override val currentHost: String
        get() = networkProvider.host

    override suspend fun update() {
        val transactionIds = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .mapNotNull { it.hash }
        when (val response = networkProvider.getInfo(ErgoAddressRequestData(wallet.address, transactionIds))) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private suspend fun updateWallet(response: ErgoAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance.toString()}")
        wallet.amounts[AmountType.Coin]?.value =
            response.balance.toBigDecimal().movePointLeft(blockchain.decimals())

        wallet.recentTransactions.forEach() {
            if (response.TransactionsId.contains(it.hash)) {
                it.status = TransactionStatus.Confirmed
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (val blockResult = networkProvider.getLastBlock()) {
            is Result.Failure -> SimpleResult.Failure(blockResult.error)
            is Result.Success -> when (val unspentOutput = networkProvider.getUnspent(wallet.address)) {
                is Result.Failure -> SimpleResult.Failure(unspentOutput.error)
                is Result.Success -> {
                    val myBoxes = unspentOutput.data.map { it.toErgoBox() }.sortedBy { it.value }
                    val recipient = Address(
                        transactionData.destinationAddress,
                        transactionData.destinationAddress.decodeBase58()
                    )
                    val payloadOuts = arrayListOf(
                        ErgoBox(
                            id = "",
                            transactionData.amount.longValue,
                            blockResult.data.items!!.first().height!! - 720L,
                            ErgoUtils.ergoTree(recipient),
                            recipient
                        )
                    )

                    val ergValue = transactionData.fee!!.longValue!! + transactionData.amount.longValue!!
                    val remains = hashMapOf("ERG" to ergValue)
                    val boxToSpend = arrayListOf<ErgoBox>()

                    for (box in myBoxes) {
                        boxToSpend.add(box)

                        if (remains["ERG"]!! > 0) {
                            remains["ERG"] = remains["ERG"]!! - box.value!!
                        }
                        if (remains["ERG"]!! < 0)
                            break
                    }

                    val mainNetFeeAddress = Address(
                        feeMainNetAddress,
                        feeMainNetAddress.decodeBase58()
                    )
                    val feeBox = (ErgoBox(
                        id = "",
                        transactionData.fee.longValue, blockResult.data.items!!.first().height!! - 720L, ErgoUtils
                            .ergoTree
                                (mainNetFeeAddress),
                        mainNetFeeAddress
                    ))

                    payloadOuts.add(feeBox)

                    val totalValueIn = boxToSpend.sumOf { it.value!! }
                    val totalValueOut = payloadOuts.sumOf { it.value!! }
                    val changeValue = totalValueIn - totalValueOut
                    if (changeValue < 0)
                        throw Exception("Not enough funds")
                    else if (changeValue > 0) {
                        val changeAddress = Address(
                            transactionData.sourceAddress,
                            transactionData.sourceAddress.decodeBase58()
                        )
                        val changeOutput =
                            ErgoBox(
                                "", totalValueIn - totalValueOut,
                                blockResult.data.items!!.first()
                                    .height,
                                ErgoUtils.ergoTree(changeAddress), changeAddress
                            )
                        payloadOuts.add(changeOutput)
                    }

                    val hash = Transaction(
                        inputs = boxToSpend.map { it.toInput() },
                        outputs = payloadOuts
                    ).toHash()

                    return when (val signerResponse = signer.sign(hash, wallet.publicKey)) {
                        is CompletionResult.Success -> {
                            when (val result = networkProvider.sendTransaction(signerResponse.data.encodeBase58())) {
                                is Result.Failure -> SimpleResult.Failure(result.error)
                                is Result.Success -> SimpleResult.Success
                            }

                        }
                        is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
                    }
                }
            }

        }
        /*   const myBoxes = yield this.explorer.getUnspentOutputs(address_1.Address.fromSk(sk));
           const payloadOuts = [new ergoBox_1.ErgoBox('', amountInt, height - constants_1.heightDelta, new address_1.Address(recipient))];
           const boxesToSpend = ergoBox_1.ErgoBox.getSolvingBoxes(myBoxes, payloadOuts);
           const unsignedTx = transaction_1.Transaction.fromOutputs(boxesToSpend, payloadOuts);
           const signedTx = unsignedTx.sign(sk);
           return yield this.explorer.broadcastTx(signedTx);*/
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return Result.Success(listOf(Amount(BigDecimal.valueOf(0.001), blockchain)))
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }
}
