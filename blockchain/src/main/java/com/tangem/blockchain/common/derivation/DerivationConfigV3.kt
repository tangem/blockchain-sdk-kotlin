package com.tangem.blockchain.common.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Derivation config for Wallet 2.0
 *
 * Types:
 * - `Stellar`, `Solana`, `TON`. According to `SEP0005`
 * https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0005.md
 * - `Polkadot`, `Kusama` used to all nodes in the path is hardened
 * https://wiki.polkadot.network/docs/learn-account-advanced#derivation-paths
 * - `Cardano`. According to  `CIP1852`
 * https://cips.cardano.org/cips/cip1852/
 * - `Bitcoin`, `Litecoin`. Default address is `SegWit`. According to `BIP-84`
 * https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki
 * - `EVM-like` without `Ethereum classic` with `Ethereum` coinType(60).
 * - `All else`. According to `BIP44`
 * https://github.com/satoshilabs/slips/blob/master/slip-0044.md
 */

object DerivationConfigV3 : DerivationConfig() {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun derivations(blockchain: Blockchain): Map<AddressType, DerivationPath> {
        return when (blockchain) {
            Blockchain.Bitcoin -> {
                mapOf(AddressType.Default to DerivationPath("m/84'/0'/0'/0/0"))
            }
            Blockchain.Litecoin -> {
                mapOf(AddressType.Default to DerivationPath("m/84'/2'/0'/0/0"))
            }

            Blockchain.Stellar -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/148'/0'"))
            }

            Blockchain.Solana -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/501'/0'"))
            }

            Blockchain.Cardano -> {
                mapOf(AddressType.Default to DerivationPath("m/1852'/1815'/0'/0/0"))
            }
            Blockchain.BitcoinCash -> {
                mapOf(
                    AddressType.Default to DerivationPath("m/44'/145'/0'/0/0"),
                    AddressType.Legacy to DerivationPath("m/44'/145'/0'/0/0"),
                )
            }

            Blockchain.Ethereum,
            Blockchain.EthereumPow,
            Blockchain.EthereumFair,
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
            -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/60'/0'/0/0"))
            }

            Blockchain.XDC -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/550'/0'/0/0"))
            }

            Blockchain.EthereumClassic -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/61'/0'/0/0"))
            }
            Blockchain.Binance -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/714'/0'/0/0"))
            }
            Blockchain.XRP -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/144'/0'/0/0"))
            }
            Blockchain.Ducatus -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/0'/0'/0/0"))
            }
            Blockchain.Tezos -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/1729'/0'/0'"))
            }
            Blockchain.Dogecoin -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/3'/0'/0/0"))
            }
            Blockchain.Polkadot -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/354'/0'/0'/0'"))
            }
            Blockchain.Kusama -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/434'/0'/0'/0'"))
            }
            Blockchain.AlephZero -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/643'/0'/0'/0'"))
            }
            Blockchain.Tron -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/195'/0'/0/0"))
            }
            Blockchain.Dash -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/5'/0'/0/0"))
            }
            Blockchain.TON -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/607'/0'"))
            }
            Blockchain.Kaspa -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/111111'/0'/0/0"))
            }
            Blockchain.Ravencoin -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/175'/0'/0/0"))
            }
            Blockchain.Cosmos -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/118'/0'/0/0"))
            }
            Blockchain.TerraV1, Blockchain.TerraV2 -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/330'/0'/0/0"))
            }
            Blockchain.Near -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/397'/0'"))
            }
            Blockchain.VeChain, Blockchain.VeChainTestnet -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/818'/0'/0/0"))
            }
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
            -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/1'/0'/0/0"))
            }

            Blockchain.Aptos, Blockchain.AptosTestnet -> {
                mapOf(AddressType.Default to DerivationPath("m/44'/637'/0'/0'/0'"))
            }
        }
    }
}
