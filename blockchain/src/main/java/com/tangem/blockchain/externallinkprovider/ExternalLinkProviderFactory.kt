package com.tangem.blockchain.externallinkprovider

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.externallinkprovider.providers.*

internal object ExternalLinkProviderFactory {

    @Suppress("CyclomaticComplexMethod")
    fun makeProvider(blockchain: Blockchain): ExternalLinkProvider {
        val isTestnet = blockchain.isTestnet()
        return when (blockchain) {
            Blockchain.Unknown -> error("There is no ExternalLinkProvider for Blockchain.Unknown")
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> ArbitrumExternalLinkProvider(isTestnet)
            Blockchain.Avalanche, Blockchain.AvalancheTestnet -> AvalancheExternalLinkProvider(isTestnet)
            Blockchain.Binance, Blockchain.BinanceTestnet -> BinanceExternalLinkProvider(isTestnet)
            Blockchain.BSC, Blockchain.BSCTestnet -> BSCExternalLinkProvider(isTestnet)
            Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> BitcoinExternalLinkProvider(isTestnet)
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> BitcoinCashExternalLinkProvider(isTestnet)
            Blockchain.Cardano -> CardanoExternalLinkProvider()
            Blockchain.Cosmos, Blockchain.CosmosTestnet -> CosmosExternalLinkProvider(isTestnet)
            Blockchain.Dogecoin -> DogecoinExternalLinkProvider()
            Blockchain.Ducatus -> DucatusExternalLinkProvider()
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> EthereumExternalLinkProvider(isTestnet)
            Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> EthereumClassicExternalLinkProvider(
                isTestnet,
            )

            Blockchain.Fantom, Blockchain.FantomTestnet -> FantomExternalLinkProvider(isTestnet)
            Blockchain.Litecoin -> LitecoinExternalLinkProvider()
            Blockchain.Near, Blockchain.NearTestnet -> NearExternalLinkProvider(isTestnet)
            Blockchain.Polkadot, Blockchain.PolkadotTestnet -> PolkadotExternalLinkProvider(isTestnet)
            Blockchain.Kava, Blockchain.KavaTestnet -> KavaExternalLinkProvider(isTestnet)
            Blockchain.Kusama -> KusamaExternalLinkProvider()
            Blockchain.Polygon, Blockchain.PolygonTestnet -> PolygonExternalLinkProvider(isTestnet)
            Blockchain.RSK -> RSKExternalLinkProvider()
            Blockchain.Stellar, Blockchain.StellarTestnet -> StellarExternalLinkProvider(isTestnet)
            Blockchain.Solana, Blockchain.SolanaTestnet -> SolanaExternalLinkProvider(isTestnet)
            Blockchain.Tezos -> TezosExternalLinkProvider()
            Blockchain.Tron, Blockchain.TronTestnet -> TronExternalLinkProvider(isTestnet)
            Blockchain.XRP -> XRPExternalLinkProvider()
            Blockchain.Gnosis -> GnosisExternalLinkProvider()
            Blockchain.Dash -> DashExternalLinkProvider()
            Blockchain.Optimism, Blockchain.OptimismTestnet -> OptimismExternalLinkProvider(isTestnet)
            Blockchain.Dischain -> EthereumFairExternalLinkProvider()
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> EthereumPowExternalLinkProvider(isTestnet)
            Blockchain.Kaspa -> KaspaExternalLinkProvider()
            Blockchain.Telos, Blockchain.TelosTestnet -> TelosExternalLinkProvider(isTestnet)
            Blockchain.TON, Blockchain.TONTestnet -> TONExternalLinkProvider(isTestnet)
            Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> RavencoinExternalLinkProvider(isTestnet)
            Blockchain.TerraV1 -> TerraV1ExternalLinkProvider()
            Blockchain.TerraV2 -> TerraV2ExternalLinkProvider()
            Blockchain.Cronos -> CronosExternalLinkProvider()
            Blockchain.AlephZero -> AlephZeroExternalLinkProvider()
            Blockchain.AlephZeroTestnet -> error("unsupported blockchain")
            Blockchain.OctaSpace -> OctaSpaceExternalLinkProvider()
            Blockchain.OctaSpaceTestnet -> error("unsupported blockchain")
            Blockchain.Chia, Blockchain.ChiaTestnet -> ChiaExternalLinkProvider(isTestnet)
            Blockchain.Decimal, Blockchain.DecimalTestnet -> DecimalExternalLinkProvider(isTestnet)
            Blockchain.XDC, Blockchain.XDCTestnet -> XDCExternalLinkProvider(isTestnet)
            Blockchain.VeChain, Blockchain.VeChainTestnet -> VeChainExternalLinkProvider(isTestnet)
            Blockchain.Aptos, Blockchain.AptosTestnet -> AptosExternalLinkProvider(isTestnet)
            Blockchain.Playa3ull -> Playa3ullExternalLinkProvider()
            Blockchain.Shibarium, Blockchain.ShibariumTestnet -> ShibariumExternalLinkProvider(isTestnet)
            Blockchain.Algorand, Blockchain.AlgorandTestnet -> AlgorandExternalLinkProvider(isTestnet)
            Blockchain.Hedera, Blockchain.HederaTestnet -> HederaExternalLinkProvider(isTestnet)
            Blockchain.Aurora, Blockchain.AuroraTestnet -> AuroraExternalLinkProvider(isTestnet)
            Blockchain.Areon, Blockchain.AreonTestnet -> AreonExternalLinkProvider()
            Blockchain.PulseChain, Blockchain.PulseChainTestnet -> PulseChainExternalLinkProvider(isTestnet)
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet -> ZkSyncEraExternalLinkProvider(isTestnet)
        }
    }
}