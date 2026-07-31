package com.tangem.blockchain.common.psbt

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.psbt.AcinqAddressCodec
import com.tangem.blockchain.blockchains.bitcoin.psbt.AcinqPsbtTransactionExtractor
import com.tangem.blockchain.blockchains.bitcoin.psbt.BitcoinPsbtProvider
import com.tangem.blockchain.blockchains.bitcoin.psbt.BitcoinjAddressCodec
import com.tangem.blockchain.blockchains.bitcoin.psbt.DefaultSighashStrategy
import com.tangem.blockchain.blockchains.bitcoincash.psbt.BitcoinCashAddressCodec
import com.tangem.blockchain.blockchains.bitcoincash.psbt.BitcoinCashPsbtTransactionExtractor
import com.tangem.blockchain.blockchains.bitcoincash.psbt.BitcoinCashSighashStrategy
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet

/**
 * Resolves the [PsbtProvider] implementation for a given [Blockchain].
 */
internal object PsbtProviderFactory {

    fun make(blockchain: Blockchain, wallet: Wallet, networkProvider: BitcoinNetworkProvider): PsbtProvider =
        when (blockchain) {
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet ->
                BitcoinPsbtProvider(
                    wallet = wallet,
                    networkProvider = networkProvider,
                    addressCodec = AcinqAddressCodec(blockchain),
                    sighashStrategy = DefaultSighashStrategy,
                    transactionExtractor = AcinqPsbtTransactionExtractor,
                )
            Blockchain.Litecoin, Blockchain.Dogecoin, Blockchain.Dash ->
                BitcoinPsbtProvider(
                    wallet = wallet,
                    networkProvider = networkProvider,
                    addressCodec = BitcoinjAddressCodec(blockchain),
                    sighashStrategy = DefaultSighashStrategy,
                    transactionExtractor = AcinqPsbtTransactionExtractor,
                )
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet ->
                BitcoinPsbtProvider(
                    wallet = wallet,
                    networkProvider = networkProvider,
                    addressCodec = BitcoinCashAddressCodec(blockchain),
                    sighashStrategy = BitcoinCashSighashStrategy(blockchain),
                    transactionExtractor = BitcoinCashPsbtTransactionExtractor,
                )
            else -> DefaultPsbtProvider
        }
}