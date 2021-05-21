package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import org.bitcoinj.core.TransactionInput.NO_SEQUENCE
import java.math.BigDecimal

class LitecoinWalletManager(
        wallet: Wallet,
        transactionBuilder: BitcoinTransactionBuilder,
        networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(wallet, transactionBuilder, networkProvider), TransactionSender {
    override val minimalFeePerKb = DEFAULT_MINIMAL_FEE_PER_KB.toBigDecimal()

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner) =
            send(transactionData, signer, NO_SEQUENCE)

    override suspend fun isPushAvailable(transactionHash: String) = Result.Success(false)
}