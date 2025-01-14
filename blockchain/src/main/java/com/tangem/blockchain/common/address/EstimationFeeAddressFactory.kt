package com.tangem.blockchain.common.address

import com.tangem.blockchain.common.Blockchain

/**
 * This entity provides addresses for calculating commissions in swap scenarios.
 *
 * If the blockchain lacks an "initialized account state", it generates a random address.
 *
 * If the commission for sending tokens to a “warmed-up” account differs from that for sending to an empty account,
 * the address of the “warmed-up” account should be set (e.g., in Cardano).
 *
 * If the fee does not depend on the amount or destination, or if the fee is fixed, an empty address can be set to
 * avoid wasting resources on address generation.
 */
class EstimationFeeAddressFactory {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun makeAddress(blockchain: Blockchain): String {
        return when (blockchain) {
            Blockchain.Unknown, // shouldn't get there
            Blockchain.Ducatus, // doesn't depend on destination
            Blockchain.Tezos, // doesn't depend on destination
            Blockchain.Hedera, Blockchain.HederaTestnet, // doesn't depend on destination
            Blockchain.Chia, Blockchain.ChiaTestnet, // doesn't depend on destination
            Blockchain.InternetComputer, // fixed
            Blockchain.Casper, Blockchain.CasperTestnet, // fixed
            Blockchain.XRP,
            Blockchain.Stellar, Blockchain.StellarTestnet,
            Blockchain.Binance, Blockchain.BinanceTestnet,
            Blockchain.SolanaTestnet,
            -> ""

            Blockchain.Nexa, Blockchain.NexaTestnet,
            -> TODO("Not implemented")

            Blockchain.Cardano -> CARDANO_ESTIMATION_ADDRESS

            // We have to generate a new dummy address for UTXO-like
            Blockchain.Bitcoin,
            Blockchain.BitcoinTestnet,
            -> "bc1qkrc5kmpq546wr2xk0errg58yw9jjq7thvhdk5k"
            Blockchain.Litecoin -> "MSqjXH6toL4kHqsRo3mWaWMkhmiH9GQxLR"
            Blockchain.BitcoinCash,
            Blockchain.BitcoinCashTestnet,
            -> "bitcoincash:qrn96yyxa93t6sqmehvls6746qafkcsuku6zmd9460"
            Blockchain.Dogecoin -> "DRVD4B4YD9CBSjqaa3UfF42vSN6k2tJwhz"
            Blockchain.Dash -> "Xqfekbgca2HDaXhrNYP2HTnuQ5go2E8dDE"
            Blockchain.Ravencoin,
            Blockchain.RavencoinTestnet,
            -> "RT5qKgXdmh9pqtz71cgfL834VfeXFVH1sG"
            Blockchain.Solana -> "9wuDg6Y4H4j86Kg5aUGrUeaBa3sAUzjMs37KbeGFnRuM"
            Blockchain.Radiant -> "1K8jBuCKzuwvFCjL7Qpqq69k1hnVXJ31Nc"
            Blockchain.Fact0rn -> "fact1q69h3nzh7rl2uv09zp5pw26vw58wdcl2j4lyag0"
            // EVM-like
            Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet ->
                "0xc49722a6f4Fe5A1347710dEAAa1fafF4c275689b"
            Blockchain.Decimal, Blockchain.DecimalTestnet ->
                "d0122a5qy59f7qge7d6hkz4u389qmd0dsrh6a7qnx"
            Blockchain.Ethereum, Blockchain.EthereumTestnet,
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet,
            Blockchain.Dischain,
            Blockchain.RSK,
            Blockchain.BSC, Blockchain.BSCTestnet,
            Blockchain.Polygon, Blockchain.PolygonTestnet,
            Blockchain.Avalanche, Blockchain.AvalancheTestnet,
            Blockchain.Fantom, Blockchain.FantomTestnet,
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet,
            Blockchain.Gnosis,
            Blockchain.Optimism, Blockchain.OptimismTestnet,
            Blockchain.Kava, Blockchain.KavaTestnet,
            Blockchain.Cronos,
            Blockchain.Telos, Blockchain.TelosTestnet,
            Blockchain.Shibarium, Blockchain.ShibariumTestnet,
            Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet,
            Blockchain.Playa3ull,
            Blockchain.Aurora, Blockchain.AuroraTestnet,
            Blockchain.Areon, Blockchain.AreonTestnet,
            Blockchain.PulseChain, Blockchain.PulseChainTestnet,
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet,
            Blockchain.Base, Blockchain.BaseTestnet,
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
            Blockchain.Manta, Blockchain.MantaTestnet,
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet,
            Blockchain.Moonriver, Blockchain.MoonriverTestnet,
            Blockchain.Mantle, Blockchain.MantleTestnet,
            Blockchain.Flare, Blockchain.FlareTestnet,
            Blockchain.Taraxa, Blockchain.TaraxaTestnet,
            Blockchain.Blast, Blockchain.BlastTestnet,
            Blockchain.Cyber, Blockchain.CyberTestnet,
            Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet,
            Blockchain.Core, Blockchain.CoreTestnet,
            Blockchain.Chiliz, Blockchain.ChilizTestnet,
            Blockchain.VanarChain, Blockchain.VanarChainTestnet,
            Blockchain.Xodex,
            Blockchain.Canxium,
            Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet,
            -> "0x52bb4012854f808CF9BAbd855e44E506dAf6C077"
            // Polkadot-like
            Blockchain.Polkadot, Blockchain.PolkadotTestnet ->
                "15RRtiC2akPUE9FGqqa66awoAFz6XCnZiFUf34k2CHbLWNfC"
            Blockchain.Kusama ->
                "CsNtwDXUzMR4ZKBQrXCfA6bBXQBFU1DDbtSwLAsaVr13sGs"
            Blockchain.Bittensor -> "5HLcF8UkyCTK5oszoTxx8LKxEzmtEEfPWeAxCz5NiDjqWH9y"
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet ->
                "5DaWppqEJPc6BhFKD2NBC1ACXPDMPYfv2AQDB5uH5KT4mpef"
            Blockchain.Joystream -> "j4SXkX46sABwjxeuzicd2e5m8gDu4ieoWHW3aggbBKkh4WvtF"
            // Cosmos-like
            Blockchain.Cosmos, Blockchain.CosmosTestnet ->
                "cosmos1lhjvds604fvac32j4eygpr820lyc82dlyq70m5"
            Blockchain.TerraV1,
            Blockchain.TerraV2,
            -> "terra1pfamr0t2daet92grdvxqex235q58qrx6xclldg"
            // Others
            Blockchain.Tron, Blockchain.TronTestnet ->
                "TA4Tkaj2nAJjkVbDHdUQDxYCbLfsZzS8pA"
            Blockchain.TON, Blockchain.TONTestnet ->
                "EQAY92urFDKejoDRdi_EfRKLGB1JkGjD8z1inj_DhgBaD0Xo"
            Blockchain.Near, Blockchain.NearTestnet ->
                "4a9fb267a005b7e923233b59aff1b73e577347a1ab36aa231a1880a91776c416"
            Blockchain.XDC, Blockchain.XDCTestnet ->
                "xdc9606Af4939f6F9fb9731A39a32B00aD966348ED6"
            Blockchain.VeChain, Blockchain.VeChainTestnet ->
                "0x1C5B4935709583758BE5b9ECeeBaf5cD6AFecF41"
            Blockchain.Aptos, Blockchain.AptosTestnet ->
                "0x4626b7ef23fb2800a0e224e8249f47e0db3579070262da2a7efb0bc52c882867"
            Blockchain.Algorand, Blockchain.AlgorandTestnet ->
                "CW6XDCKQAZUGAIOTGE2NEPYFFVW6H6IKFOTOF3W5WDUVHH4ZIDCIKYDPXY"
            Blockchain.Koinos, Blockchain.KoinosTestnet,
            -> "1C423Vbd44zjghhJR5fKJdLFS3rgVFUc9A"
            Blockchain.Filecoin -> "f1wxdu6d25dc4hmebdfgriswooum22plhmmpxibzq"
            Blockchain.Kaspa -> "kaspa:qyp2f0ust8wyvuvqrzajvehx5jyh43vcjgessjdkw9vyw6rww4fdlsgzysspfuq"
            Blockchain.Sei, Blockchain.SeiTestnet,
            -> "sei1lhjvds604fvac32j4eygpr820lyc82dlfv0ea4"
            Blockchain.Sui, Blockchain.SuiTestnet,
            -> "0xbca45e36a271e106546c89984108685215724e488570a0049a187c473cd521bc"
            Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet,
            -> "5CogUCbb5PYYbEHhDVGDN6JRRYBkd4sFRVc4wwP8oy5Su34Z"
            Blockchain.Clore -> "AJfAu7RJxiTowM9qVaTbVuS5JCPCpV3p7M"
        }
    }

    companion object {
        private const val CARDANO_ESTIMATION_ADDRESS =
            "addr1q95pg4z9tf26r5dwf72vmh62u3pr9sewq2waahyhpjzm3enz43pvhh0us3z0z5xen2skq200e67eu89s5v2s0sdh3fnsm9lknu"
    }
}