package com.tangem.blockchain.common.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Derivation config for Wallet v1 (except ac01/ac02)
 *
 * Types:
 * - `Stellar`, `Solana`. According to `SEP0005`
 * https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0005.md
 * - `Cardano`.  According to  `CIP1852`
 * https://cips.cardano.org/cips/cip1852/
 * - `EVM-like` with `Ethereum` coinType(60).
 * - `All else`. According to `BIP44`
 * https://github.com/satoshilabs/slips/blob/master/slip-0044.md
 */
object DerivationConfigV2 : DerivationConfig() {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun derivations(blockchain: Blockchain): Map<AddressType, DerivationPath> {
        return when (blockchain) {
            Blockchain.Bitcoin -> mapOf(
                AddressType.Legacy to DerivationPath("m/44'/0'/0'/0/0"),
                AddressType.Default to DerivationPath("m/44'/0'/0'/0/0"),
            )
            Blockchain.Litecoin -> mapOf(
                AddressType.Legacy to DerivationPath("m/44'/2'/0'/0/0"),
                AddressType.Default to DerivationPath("m/44'/2'/0'/0/0"),
            )
            Blockchain.Stellar -> mapOf(AddressType.Default to DerivationPath("m/44'/148'/0'"))
            Blockchain.Solana -> mapOf(AddressType.Default to DerivationPath("m/44'/501'/0'"))
            Blockchain.Cardano -> mapOf(
                AddressType.Default to DerivationPath("m/1852'/1815'/0'/0/0"),
                AddressType.Legacy to DerivationPath("m/1852'/1815'/0'/0/0"),
            )
            Blockchain.BitcoinCash -> mapOf(
                AddressType.Legacy to DerivationPath("m/44'/145'/0'/0/0"),
                AddressType.Default to DerivationPath("m/44'/145'/0'/0/0"),
            )
            Blockchain.Ethereum,
            Blockchain.EthereumTestnet,
            Blockchain.EthereumPow,
            Blockchain.Dischain,
            Blockchain.EthereumClassic,
            Blockchain.RSK,
            Blockchain.BSC,
            Blockchain.Polygon,
            Blockchain.Avalanche,
            Blockchain.Fantom,
            Blockchain.Arbitrum,
            Blockchain.Gnosis,
            Blockchain.Optimism,
            Blockchain.Kava,
            Blockchain.Cronos,
            Blockchain.Telos,
            Blockchain.OctaSpace,
            Blockchain.Decimal,
            Blockchain.Playa3ull,
            Blockchain.Shibarium,
            Blockchain.Aurora,
            Blockchain.Areon,
            Blockchain.PulseChain,
            Blockchain.ZkSyncEra,
            Blockchain.Moonbeam,
            Blockchain.Manta,
            Blockchain.PolygonZkEVM,
            Blockchain.Base,
            Blockchain.Moonriver,
            Blockchain.Mantle,
            Blockchain.Flare,
            Blockchain.Taraxa,
            Blockchain.Blast,
            Blockchain.Cyber,
            Blockchain.EnergyWebChain,
            Blockchain.Core,
            Blockchain.Xodex,
            Blockchain.Canxium,
            Blockchain.Chiliz,
            Blockchain.VanarChain,
            Blockchain.OdysseyChain,
            Blockchain.Bitrock,
            Blockchain.Sonic,
            Blockchain.ApeChain,
            Blockchain.Scroll,
            Blockchain.ZkLinkNova,
            Blockchain.Hyperliquid,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/60'/0'/0/0"))
            Blockchain.XDC -> mapOf(AddressType.Default to DerivationPath("m/44'/550'/0'/0/0"))
            Blockchain.Binance -> mapOf(AddressType.Default to DerivationPath("m/44'/714'/0'/0/0"))
            Blockchain.XRP -> mapOf(AddressType.Default to DerivationPath("m/44'/144'/0'/0/0"))
            Blockchain.Ducatus -> mapOf(AddressType.Default to DerivationPath("m/44'/0'/0'/0/0"))
            Blockchain.Tezos -> mapOf(AddressType.Default to DerivationPath("m/44'/1729'/0'/0/0"))
            Blockchain.Dogecoin -> mapOf(AddressType.Default to DerivationPath("m/44'/3'/0'/0/0"))
            Blockchain.Polkadot -> mapOf(AddressType.Default to DerivationPath("m/44'/354'/0'/0/0"))
            Blockchain.Kusama -> mapOf(AddressType.Default to DerivationPath("m/44'/434'/0'/0/0"))
            Blockchain.AlephZero -> mapOf(AddressType.Default to DerivationPath("m/44'/643'/0'/0'/0'"))
            Blockchain.Joystream -> mapOf(AddressType.Default to DerivationPath("m/44'/1014'/0'/0'/0'"))
            Blockchain.Bittensor -> mapOf(AddressType.Default to DerivationPath("m/44'/1005'/0'/0'/0'"))
            Blockchain.Tron -> mapOf(AddressType.Default to DerivationPath("m/44'/195'/0'/0/0"))
            Blockchain.Dash -> mapOf(AddressType.Default to DerivationPath("m/44'/5'/0'/0/0"))
            Blockchain.TON -> mapOf(AddressType.Default to DerivationPath("m/44'/607'/0'/0/0"))
            Blockchain.Kaspa -> mapOf(AddressType.Default to DerivationPath("m/44'/111111'/0'/0/0"))
            Blockchain.Ravencoin -> mapOf(AddressType.Default to DerivationPath("m/44'/175'/0'/0/0"))
            Blockchain.Cosmos -> mapOf(AddressType.Default to DerivationPath("m/44'/118'/0'/0/0"))
            Blockchain.TerraV1, Blockchain.TerraV2 -> mapOf(AddressType.Default to DerivationPath("m/44'/330'/0'/0/0"))
            Blockchain.Near -> mapOf(AddressType.Default to DerivationPath("m/44'/397'/0'"))
            Blockchain.VeChain,
            Blockchain.VeChainTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/818'/0'/0/0"))
            Blockchain.Algorand,
            Blockchain.AlgorandTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/283'/0'/0'/0'"))
            Blockchain.Hedera -> mapOf(AddressType.Default to DerivationPath("m/44'/3030'/0'/0'/0'"))
            Blockchain.Chia, Blockchain.ChiaTestnet -> mapOf(AddressType.Default to DerivationPath(""))
            Blockchain.Unknown,
            Blockchain.ArbitrumTestnet,
            Blockchain.AvalancheTestnet,
            Blockchain.BinanceTestnet,
            Blockchain.BSCTestnet,
            Blockchain.BitcoinTestnet,
            Blockchain.BitcoinCashTestnet,
            Blockchain.CosmosTestnet,
            Blockchain.EthereumClassicTestnet,
            Blockchain.FantomTestnet,
            Blockchain.PolkadotTestnet,
            Blockchain.KavaTestnet,
            Blockchain.PolygonTestnet,
            Blockchain.StellarTestnet,
            Blockchain.SolanaTestnet,
            Blockchain.TronTestnet,
            Blockchain.OptimismTestnet,
            Blockchain.EthereumPowTestnet,
            Blockchain.TONTestnet,
            Blockchain.RavencoinTestnet,
            Blockchain.TelosTestnet,
            Blockchain.AlephZeroTestnet,
            Blockchain.OctaSpaceTestnet,
            Blockchain.NearTestnet,
            Blockchain.DecimalTestnet,
            Blockchain.XDCTestnet,
            Blockchain.ShibariumTestnet,
            Blockchain.HederaTestnet,
            Blockchain.AuroraTestnet,
            Blockchain.AreonTestnet,
            Blockchain.PulseChainTestnet,
            Blockchain.ZkSyncEraTestnet,
            Blockchain.MoonbeamTestnet,
            Blockchain.MantaTestnet,
            Blockchain.PolygonZkEVMTestnet,
            Blockchain.BaseTestnet,
            Blockchain.MoonriverTestnet,
            Blockchain.MantleTestnet,
            Blockchain.FlareTestnet,
            Blockchain.TaraxaTestnet,
            Blockchain.BlastTestnet,
            Blockchain.CyberTestnet,
            Blockchain.EnergyWebChainTestnet,
            Blockchain.CoreTestnet,
            Blockchain.ChilizTestnet,
            Blockchain.VanarChainTestnet,
            Blockchain.OdysseyChainTestnet,
            Blockchain.BitrockTestnet,
            Blockchain.SonicTestnet,
            Blockchain.ApeChainTestnet,
            Blockchain.KaspaTestnet,
            Blockchain.ScrollTestnet,
            Blockchain.ZkLinkNovaTestnet,
            Blockchain.HyperliquidTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/1'/0'/0/0"))
            Blockchain.Aptos,
            Blockchain.AptosTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/637'/0'/0'/0'"))
            Blockchain.Nexa,
            Blockchain.NexaTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/29223'/0'/0/0"))
            Blockchain.Radiant -> mapOf(AddressType.Default to DerivationPath("m/44'/512'/0'/0/0"))
            Blockchain.Fact0rn -> mapOf(AddressType.Default to DerivationPath("m/84'/42069'/0'/0/0"))
            Blockchain.Koinos,
            Blockchain.KoinosTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/659'/0'/0/0"))
            Blockchain.Filecoin -> mapOf(AddressType.Default to DerivationPath("m/44'/461'/0'/0/0"))
            Blockchain.Sei,
            Blockchain.SeiTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/118'/0'/0/0"))
            Blockchain.InternetComputer -> mapOf(AddressType.Default to DerivationPath("m/44'/223'/0'/0/0"))
            Blockchain.Sui,
            Blockchain.SuiTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/784'/0'/0'/0'"))
            Blockchain.EnergyWebX,
            Blockchain.EnergyWebXTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/246'/0'/0'/0'"))
            Blockchain.Casper,
            Blockchain.CasperTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/506'/0'/0/0"))
            Blockchain.Alephium,
            Blockchain.AlephiumTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/1234'/0'/0/0"))
            Blockchain.Clore -> mapOf(AddressType.Default to DerivationPath("m/44'/1313'/0'/0/0"))
            Blockchain.Pepecoin,
            Blockchain.PepecoinTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/3434'/0'/0/0"))
            Blockchain.Quai,
            Blockchain.QuaiTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/994'/0'/0"))
        }
    }
}