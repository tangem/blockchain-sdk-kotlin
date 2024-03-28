package com.tangem.blockchain.blockchains.radiant

import android.util.Log
import com.tangem.blockchain.blockchains.radiant.models.RadiantAccountInfo
import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.math.RoundingMode

internal class RadiantWalletManager(
    wallet: Wallet,
    networkProviders: List<ElectrumNetworkProvider>,
) : WalletManager(wallet) {

    private val networkService = RadiantNetworkService(networkProviders)
    private val blockchain = wallet.blockchain
    private val addressScriptHash by lazy { generateAddressScriptHash(walletAddress = wallet.address) }

    override val currentHost: String get() = networkService.baseUrl

    override suspend fun updateInternal() {
        when (val result = networkService.getInfo(address = wallet.address, scriptHash = addressScriptHash)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(accountModel: RadiantAccountInfo) {
        wallet.setCoinValue(accountModel.balance)
        // transactionBuilder.setUnspentOutputs(accountModel.unspentOutputs)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return try {
            when (val feeResult = networkService.getEstimatedFee(REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS)) {
                is Result.Failure -> return feeResult
                is Result.Success -> {
                    val transactionSize = 1661.toBigDecimal() // FIXME: Change with real value
                    val minFee = feeResult.data.minimalPerKb.calculateFee(transactionSize)
                    val normalFee = feeResult.data.normalPerKb.calculateFee(transactionSize)
                    val priorityFee = feeResult.data.priorityPerKb.calculateFee(transactionSize)
                    val fees = TransactionFee.Choosable(
                        minimum = Fee.Common(Amount(minFee, blockchain)),
                        normal = Fee.Common(Amount(normalFee, blockchain)),
                        priority = Fee.Common(Amount(priorityFee, blockchain)),
                    )
                    Result.Success(fees)
                }
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun BigDecimal.calculateFee(txSize: BigDecimal): BigDecimal = this
        .multiply(txSize)
        .setScale(wallet.blockchain.decimals(), RoundingMode.UP)

    private fun generateAddressScriptHash(walletAddress: String): String {
        val address = LegacyAddress.fromBase58(RadiantMainNetParams(), walletAddress)
        val p2pkhScript = ScriptBuilder.createOutputScript(address)
        val sha256Hash = Sha256Hash.hash(p2pkhScript.program)
        return sha256Hash.reversedArray().toHexString()
    }

    private companion object {
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS = 10
    }
}