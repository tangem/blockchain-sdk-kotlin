package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptBuilder
import java.math.RoundingMode

class NexaWalletManager(
    wallet: Wallet,
    private val networkProvider: ElectrumNetworkProvider,
) : WalletManager(wallet) {
    override val currentHost: String
        get() = networkProvider.baseUrl

    private val bitcoinAddressService = BitcoinAddressService(Blockchain.Bitcoin)

    private val addressScriptHash: String by lazy {
        val legacyAddress = bitcoinAddressService.makeLegacyAddress(wallet.publicKey.blockchainKey)
        val address = LegacyAddress.fromBase58(MainNetParams(), legacyAddress.value)
        val p2pkhScript = ScriptBuilder.createOutputScript(address)
        val sha256Hash = Sha256Hash.hash(p2pkhScript.program)
        sha256Hash.reversedArray().toHexString()
    }

    override suspend fun updateInternal() {
        val accountRes = networkProvider.getAccount(addressScriptHash)

        val account = accountRes.successOr { throw it.error }

        wallet.setCoinValue(account.confirmedAmount)

        // TODO
        // val outputsRes = networkProvider.getUnspentUTXOs(addressScriptHash)
        // val outputs = outputsRes.successOr { throw it.error }
        //
        // val bitcoinUnspentOut = outputs
        //     .filter {
        //         it.isConfirmed
        //     }.map {record ->
        //         // val transaction = networkProvider.getTransaction(record.txHash)
        //         //     .successOr { throw it.error }
        //
        //         BitcoinUnspentOutput(
        //             amount = record.value,
        //             outputIndex = record.txPos,
        //             transactionHash = Sha256Hash.wrap(record.txHash).reversedBytes,
        //             outputScript = addressScriptHash.encodeToByteArray(),
        //         )
        //     }
        //
        // transactionBuilder.unspentOutputs = bitcoinUnspentOut
        // outputsCount = bitcoinUnspentOut.size
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        TODO()
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val transactionSize = TEST_TRANSACTION_SIZE // TODO

        // TODO research fee rates
        return networkProvider.getEstimateFee(REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS).map {
            val fee = (it.feeInCoinsPer1000Bytes ?: DEFAULT_FEE_IN_COINS_PER_1000_BYTES)
                .divide(KB_DIVIDER)
                .multiply(transactionSize.toBigDecimal())
                .multiply(NORMAL_FEE_RATE)
                .setScale(Blockchain.Nexa.decimals(), RoundingMode.DOWN)

            TransactionFee.Single(
                normal = Fee.Common(Amount(fee, blockchain = Blockchain.Nexa)),
            )
        }
    }

    companion object {
        private const val TEST_TRANSACTION_SIZE = 256 // TODO delete
        private val DEFAULT_FEE_IN_COINS_PER_1000_BYTES = 1000.toBigDecimal()
        private val NORMAL_FEE_RATE = 0.03.toBigDecimal()
        private val KB_DIVIDER = 1000.toBigDecimal()
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS = 332
    }
}