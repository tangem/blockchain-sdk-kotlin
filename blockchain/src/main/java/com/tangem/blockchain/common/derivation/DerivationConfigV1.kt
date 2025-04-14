package com.tangem.blockchain.common.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Derivation config for ac01/ac02 cards
 *
 * Types:
 * - `Stellar`, `Solana`. According to `SEP0005`
 * https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0005.md
 * - `Cardano`. According to  `CIP1852`
 * https://cips.cardano.org/cips/cip1852/
 * - `All else`. According to `BIP44`
 * https://github.com/satoshilabs/slips/blob/master/slip-0044.md
 *
 * For EVM if not found in slip-0044.md -> use 60 coin type
 */

object DerivationConfigV1 : DerivationConfig() {

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
            Blockchain.EthereumPow,
            Blockchain.Dischain,
            Blockchain.OctaSpace,
            Blockchain.Decimal,
            Blockchain.Playa3ull,
            Blockchain.Shibarium,
            Blockchain.Aurora,
            Blockchain.Areon,
            Blockchain.PulseChain,
            Blockchain.ZkSyncEra,
            Blockchain.Base,
            Blockchain.Manta,
            Blockchain.PolygonZkEVM,
            Blockchain.Mantle,
            Blockchain.Flare,
            Blockchain.Taraxa,
            Blockchain.Blast,
            Blockchain.Cyber,
            Blockchain.Canxium,
            Blockchain.Chiliz,
            Blockchain.VanarChain,
            Blockchain.Xodex,
            Blockchain.ApeChain,
            Blockchain.ZkLinkNova,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/60'/0'/0/0"))
            Blockchain.OdysseyChain -> mapOf(AddressType.Default to DerivationPath("m/44'/695'/0'/0/0"))
            Blockchain.XDC -> mapOf(AddressType.Default to DerivationPath("m/44'/550'/0'/0/0"))
            Blockchain.EthereumClassic -> mapOf(AddressType.Default to DerivationPath("m/44'/61'/0'/0/0"))
            Blockchain.RSK -> mapOf(AddressType.Default to DerivationPath("m/44'/137'/0'/0/0"))
            Blockchain.Binance -> mapOf(AddressType.Default to DerivationPath("m/44'/714'/0'/0/0"))
            Blockchain.XRP -> mapOf(AddressType.Default to DerivationPath("m/44'/144'/0'/0/0"))
            Blockchain.Ducatus -> mapOf(AddressType.Default to DerivationPath("m/44'/0'/0'/0/0"))
            Blockchain.Tezos -> mapOf(AddressType.Default to DerivationPath("m/44'/1729'/0'/0/0"))
            Blockchain.Dogecoin -> mapOf(AddressType.Default to DerivationPath("m/44'/3'/0'/0/0"))
            Blockchain.BSC -> mapOf(AddressType.Default to DerivationPath("m/44'/9006'/0'/0/0"))
            Blockchain.Polygon -> mapOf(AddressType.Default to DerivationPath("m/44'/966'/0'/0/0"))
            Blockchain.Avalanche -> mapOf(AddressType.Default to DerivationPath("m/44'/9000'/0'/0/0"))
            Blockchain.Fantom -> mapOf(AddressType.Default to DerivationPath("m/44'/1007'/0'/0/0"))
            Blockchain.Polkadot -> mapOf(AddressType.Default to DerivationPath("m/44'/354'/0'/0/0"))
            Blockchain.Kusama -> mapOf(AddressType.Default to DerivationPath("m/44'/434'/0'/0/0"))
            Blockchain.AlephZero -> mapOf(AddressType.Default to DerivationPath("m/44'/643'/0'/0'/0'"))
            Blockchain.Joystream -> mapOf(AddressType.Default to DerivationPath("m/44'/1014'/0'/0'/0'"))
            Blockchain.Bittensor -> mapOf(AddressType.Default to DerivationPath("m/44'/1005'/0'/0'/0'"))
            Blockchain.Tron -> mapOf(AddressType.Default to DerivationPath("m/44'/195'/0'/0/0"))
            Blockchain.Arbitrum -> mapOf(AddressType.Default to DerivationPath("m/44'/9001'/0'/0/0"))
            Blockchain.Dash -> mapOf(AddressType.Default to DerivationPath("m/44'/5'/0'/0/0"))
            Blockchain.Gnosis -> mapOf(AddressType.Default to DerivationPath("m/44'/700'/0'/0/0"))
            Blockchain.Optimism -> mapOf(AddressType.Default to DerivationPath("m/44'/614'/0'/0/0"))
            Blockchain.TON -> mapOf(AddressType.Default to DerivationPath("m/44'/607'/0'/0/0"))
            Blockchain.Kava -> mapOf(AddressType.Default to DerivationPath("m/44'/459'/0'/0/0"))
            Blockchain.Kaspa -> mapOf(AddressType.Default to DerivationPath("m/44'/111111'/0'/0/0"))
            Blockchain.Ravencoin -> mapOf(AddressType.Default to DerivationPath("m/44'/175'/0'/0/0"))
            Blockchain.Cosmos -> mapOf(AddressType.Default to DerivationPath("m/44'/118'/0'/0/0"))
            Blockchain.TerraV1, Blockchain.TerraV2 -> mapOf(AddressType.Default to DerivationPath("m/44'/330'/0'/0/0"))
            Blockchain.Cronos -> mapOf(AddressType.Default to DerivationPath("m/44'/10000025'/0'/0/0"))
            Blockchain.Telos -> mapOf(AddressType.Default to DerivationPath("m/44'/977'/0'/0/0"))
            Blockchain.Near -> mapOf(AddressType.Default to DerivationPath("m/44'/397'/0'"))
            Blockchain.VeChain,
            Blockchain.VeChainTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/818'/0'/0/0"))
            Blockchain.Algorand,
            Blockchain.AlgorandTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/283'/0'/0'/0'"))
            Blockchain.Hedera -> mapOf(AddressType.Default to DerivationPath("m/44'/3030'/0'/0'/0'"))
            Blockchain.Moonbeam -> mapOf(AddressType.Default to DerivationPath("m/44'/1284'/0'/0/0"))
            Blockchain.Moonriver -> mapOf(AddressType.Default to DerivationPath("m/44'/1285'/0'/0/0"))
            Blockchain.Chia, Blockchain.ChiaTestnet -> mapOf(AddressType.Default to DerivationPath(""))
            Blockchain.Unknown,
            Blockchain.ArbitrumTestnet,
            Blockchain.AvalancheTestnet,
            Blockchain.BinanceTestnet,
            Blockchain.BSCTestnet,
            Blockchain.BitcoinTestnet,
            Blockchain.BitcoinCashTestnet,
            Blockchain.CosmosTestnet,
            Blockchain.EthereumTestnet,
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
            Blockchain.SonicTestnet,
            Blockchain.ApeChainTestnet,
            Blockchain.KaspaTestnet,
            Blockchain.ZkLinkNovaTestnet,
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
            Blockchain.EnergyWebChain,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/246'/0'/0/0"))
            Blockchain.Core,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/1116'/0'/0/0"))
            Blockchain.Casper,
            Blockchain.CasperTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/506'/0'/0/0"))
            Blockchain.Alephium,
            Blockchain.AlephiumTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/1234'/0'/0/0"))
            Blockchain.Clore -> mapOf(AddressType.Default to DerivationPath("m/44'/1313'/0'/0/0"))
            Blockchain.Bitrock,
            Blockchain.BitrockTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/7171666'/0'/0/0"))
            Blockchain.Sonic -> mapOf(AddressType.Default to DerivationPath("m/44'/10007'/0'/0/0"))
            Blockchain.Scroll,
            Blockchain.ScrollTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/534352'/0'/0/0"))
            Blockchain.Pepecoin,
            Blockchain.PepecoinTestnet,
            -> mapOf(AddressType.Default to DerivationPath("m/44'/3434'/0'/0/0"))
        }
    }
}