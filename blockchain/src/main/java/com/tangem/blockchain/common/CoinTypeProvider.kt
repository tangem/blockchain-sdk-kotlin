package com.tangem.blockchain.common


object CoinTypeProvider {

    //    https://github.com/satoshilabs/slips/blob/master/slip-0044.md
    fun getCoinType(blockchain: Blockchain, style: DerivationStyle): Long {
        if (blockchain.isTestnet()) return 1

        val ethCoinType = 60L

        if (style == DerivationStyle.NEW && blockchain.isEvm()) return ethCoinType

        return when (blockchain) {
            Blockchain.Bitcoin, Blockchain.Ducatus -> 0
            Blockchain.Litecoin -> 2
            Blockchain.Dogecoin -> 3
            Blockchain.Dash -> 5
            Blockchain.Ethereum, Blockchain.EthereumPow, Blockchain.EthereumFair -> ethCoinType
            Blockchain.EthereumClassic -> 61
            Blockchain.RSK -> 137
            Blockchain.XRP -> 144
            Blockchain.BitcoinCash -> 145
            Blockchain.Stellar -> 148
            Blockchain.Polkadot -> 354
            Blockchain.Kusama -> 434
            Blockchain.Kava -> 459
            Blockchain.Solana -> 501
            Blockchain.Binance -> 714
            Blockchain.Polygon -> 966
            Blockchain.Fantom -> 1007
            Blockchain.Tezos -> 1729
            Blockchain.Cardano, Blockchain.CardanoShelley -> 1815
            Blockchain.Avalanche -> 9000
            Blockchain.Arbitrum -> 9001
            Blockchain.BSC -> 9006
            Blockchain.Tron -> 195
            Blockchain.Gnosis -> 700
            Blockchain.Optimism -> 614
            Blockchain.TON -> 607
            Blockchain.Kaspa -> 111111
            Blockchain.Ravencoin -> 175
            Blockchain.Cosmos -> 118
            Blockchain.TerraV1, Blockchain.TerraV2 -> 330
            Blockchain.Cronos -> 10000025
            Blockchain.AlephZero -> 643
            Blockchain.Telos -> 424
            Blockchain.ArbitrumTestnet,
            Blockchain.AvalancheTestnet,
            Blockchain.BinanceTestnet,
            Blockchain.BSCTestnet,
            Blockchain.BitcoinTestnet,
            Blockchain.BitcoinCashTestnet,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumClassicTestnet,
            Blockchain.FantomTestnet,
            Blockchain.PolkadotTestnet,
            Blockchain.PolygonTestnet,
            Blockchain.StellarTestnet,
            Blockchain.SolanaTestnet,
            Blockchain.TONTestnet,
            Blockchain.TronTestnet,
            Blockchain.OptimismTestnet,
            Blockchain.EthereumPowTestnet,
            Blockchain.KavaTestnet,
            Blockchain.RavencoinTestnet,
            Blockchain.CosmosTestnet,
            Blockchain.AlephZeroTestnet,
            Blockchain.TelosTestnet,
            Blockchain.Unknown,
            -> throw UnsupportedOperationException("Coin type not provided for: ${blockchain.fullName}")
        }

    }

}