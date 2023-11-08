package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.binance.BinanceAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressService
import com.tangem.blockchain.blockchains.bitcoin.BitcoinLegacyAddressService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.cardano.CardanoAddressService
import com.tangem.blockchain.blockchains.chia.ChiaAddressService
import com.tangem.blockchain.blockchains.dash.DashMainNetParams
import com.tangem.blockchain.blockchains.decimal.DecimalAddressService
import com.tangem.blockchain.blockchains.ducatus.DucatusMainNetParams
import com.tangem.blockchain.blockchains.ethereum.EthereumAddressService
import com.tangem.blockchain.blockchains.kaspa.KaspaAddressService
import com.tangem.blockchain.blockchains.polkadot.PolkadotAddressService
import com.tangem.blockchain.blockchains.ravencoin.RavencoinMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinTestNetParams
import com.tangem.blockchain.blockchains.rsk.RskAddressService
import com.tangem.blockchain.blockchains.solana.SolanaAddressService
import com.tangem.blockchain.blockchains.stellar.StellarAddressService
import com.tangem.blockchain.blockchains.tezos.TezosAddressService
import com.tangem.blockchain.blockchains.tron.TronAddressService
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.WalletCoreAddressService
import org.libdohj.params.DogecoinMainNetParams
import java.lang.IllegalStateException

class AddressServiceFactory(
    private val blockchain: Blockchain
) {

    fun makeAddressService(): AddressService {
        return when (blockchain) {
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            Blockchain.Litecoin
            -> {
                BitcoinAddressService(blockchain)
            }

            Blockchain.Dash -> {
                BitcoinLegacyAddressService(blockchain, DashMainNetParams())
            }

            Blockchain.Dogecoin -> {
                BitcoinLegacyAddressService(blockchain, DogecoinMainNetParams())
            }

            Blockchain.Ducatus -> {
                BitcoinLegacyAddressService(blockchain, DucatusMainNetParams())
            }
            Blockchain.Ravencoin -> {
                BitcoinLegacyAddressService(blockchain, RavencoinMainNetParams())
            }
            Blockchain.RavencoinTestnet -> {
                BitcoinLegacyAddressService(blockchain, RavencoinTestNetParams())
            }

            Blockchain.BitcoinCash,
            Blockchain.BitcoinCashTestnet,
            -> {
                BitcoinCashAddressService(blockchain)
            }

            Blockchain.Arbitrum,
            Blockchain.ArbitrumTestnet,
            Blockchain.Ethereum,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumClassic,
            Blockchain.EthereumClassicTestnet,
            Blockchain.BSC,
            Blockchain.BSCTestnet,
            Blockchain.Polygon,
            Blockchain.PolygonTestnet,
            Blockchain.Avalanche,
            Blockchain.AvalancheTestnet,
            Blockchain.Fantom,
            Blockchain.FantomTestnet,
            Blockchain.Gnosis,
            Blockchain.Optimism,
            Blockchain.OptimismTestnet,
            Blockchain.EthereumFair,
            Blockchain.EthereumPow,
            Blockchain.EthereumPowTestnet,
            Blockchain.Kava,
            Blockchain.KavaTestnet,
            Blockchain.Cronos,
            Blockchain.Telos,
            Blockchain.TelosTestnet,
            Blockchain.OctaSpace,
            Blockchain.OctaSpaceTestnet
            -> {
                EthereumAddressService()
            }

            Blockchain.RSK -> {
                RskAddressService()
            }

            Blockchain.Cardano -> {
                CardanoAddressService(blockchain)
            }

            Blockchain.XRP -> {
                XrpAddressService()
            }

            Blockchain.Binance -> {
                BinanceAddressService(false)
            }

            Blockchain.BinanceTestnet -> {
                BinanceAddressService(testNet = true)
            }

            Blockchain.Polkadot,
            Blockchain.PolkadotTestnet,
            Blockchain.Kusama,
            Blockchain.AlephZero,
            Blockchain.AlephZeroTestnet,
            -> {
                PolkadotAddressService(blockchain)
            }

            Blockchain.Stellar,
            Blockchain.StellarTestnet,
            -> {
                StellarAddressService()
            }

            Blockchain.Solana,
            Blockchain.SolanaTestnet,
            -> {
                SolanaAddressService()
            }

            Blockchain.Tezos -> {
                TezosAddressService()
            }

            Blockchain.TON,
            Blockchain.TONTestnet,
            Blockchain.Cosmos,
            Blockchain.CosmosTestnet,
            Blockchain.TerraV1,
            Blockchain.TerraV2,
            Blockchain.Near,
            Blockchain.NearTestnet
            -> {
                WalletCoreAddressService(blockchain)
            }

            Blockchain.Tron,
            Blockchain.TronTestnet,
            -> {
                TronAddressService()
            }

            Blockchain.Kaspa -> {
                KaspaAddressService()
            }

            Blockchain.Chia, Blockchain.ChiaTestnet -> {
                ChiaAddressService(blockchain)
            }

            Blockchain.Decimal, Blockchain.DecimalTestnet -> {
                DecimalAddressService()
            }

            Blockchain.Unknown -> {
                throw IllegalStateException("Unsupported blockchain")
            }
        }
    }
}