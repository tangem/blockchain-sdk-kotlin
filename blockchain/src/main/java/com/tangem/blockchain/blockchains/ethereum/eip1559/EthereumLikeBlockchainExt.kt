package com.tangem.blockchain.blockchains.ethereum.eip1559

import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

private val GWEI = BigDecimal.TEN.pow(9)

/**
 * Lower bound for `maxFeePerGas`, in wei.
 *
 * Arc drops transactions with `maxFeePerGas` below 20 gwei, which is its constant base fee.
 */
val Blockchain.minimalMaxFeePerGas: BigDecimal
    get() = when (this) {
        Blockchain.Arc, Blockchain.ArcTestnet -> GWEI * BigDecimal.valueOf(20)
        else -> BigDecimal.ZERO
    }

/**
 * Lower bound for `maxPriorityFeePerGas`, in wei.
 *
 * Arc reports a zero priority fee for empty blocks, so a transaction built from it would not compete with the rest
 * on a busy block.
 */
val Blockchain.minimalPriorityFeePerGas: BigDecimal
    get() = when (this) {
        Blockchain.Arc, Blockchain.ArcTestnet -> GWEI * BigDecimal.valueOf(5)
        else -> BigDecimal.ZERO
    }

/** Returns true if the blockchain supports EIP1559 */
val Blockchain.isSupportEIP1559: Boolean
    get() {
        if (!isEvm()) return false

        return when (this) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet,
            Blockchain.BSC, Blockchain.BSCTestnet,
            Blockchain.Polygon, Blockchain.PolygonTestnet,
            Blockchain.Avalanche, Blockchain.AvalancheTestnet,
            Blockchain.Fantom, Blockchain.FantomTestnet,
            Blockchain.Arbitrum, Blockchain.ArbitrumTestnet,
            Blockchain.Gnosis,
            Blockchain.Cronos,
            Blockchain.Decimal, Blockchain.DecimalTestnet,
            Blockchain.Areon, Blockchain.AreonTestnet,
            Blockchain.Playa3ull,
            Blockchain.PulseChain, Blockchain.PulseChainTestnet,
            Blockchain.Flare, Blockchain.FlareTestnet,
            Blockchain.Canxium,
            Blockchain.OdysseyChain, Blockchain.OdysseyChainTestnet,
            Blockchain.Sonic, Blockchain.SonicTestnet,
            Blockchain.ApeChain, Blockchain.ApeChainTestnet,
            Blockchain.Monad,
            Blockchain.MonadTestnet,
            Blockchain.Linea, Blockchain.LineaTestnet,
            Blockchain.ArbitrumNova,
            Blockchain.Scroll, Blockchain.ScrollTestnet,
            Blockchain.Optimism, Blockchain.OptimismTestnet,
            Blockchain.Base, Blockchain.BaseTestnet,
            Blockchain.Manta, Blockchain.MantaTestnet,
            Blockchain.Mantle, Blockchain.MantleTestnet,
            Blockchain.Hyperliquid, Blockchain.HyperliquidTestnet,
            Blockchain.Plasma, Blockchain.PlasmaTestnet,
            Blockchain.SeiEvm, Blockchain.SeiEvmTestnet,
            Blockchain.Robinhood, Blockchain.RobinhoodTestnet,
            Blockchain.Igra, Blockchain.IgraTestnet,
            Blockchain.Electroneum, Blockchain.ElectroneumTestnet,
            Blockchain.Arc, Blockchain.ArcTestnet,
            -> true
            Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet, // eth_feeHistory all zeroes
            Blockchain.EthereumPow, Blockchain.EthereumPowTestnet, // eth_feeHistory with zeros
            Blockchain.Dischain, // eth_feeHistory with zeros
            Blockchain.Kava, Blockchain.KavaTestnet, // eth_feeHistory zero or null
            Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet, // eth_feeHistory all zeroes
            Blockchain.Shibarium, Blockchain.ShibariumTestnet, // wrong base fee in eth_feeHistory. wei instead of gwei
            Blockchain.RSK,
            Blockchain.Telos, Blockchain.TelosTestnet,
            Blockchain.XDC, Blockchain.XDCTestnet,
            Blockchain.Aurora, Blockchain.AuroraTestnet,
            Blockchain.ZkSyncEra, Blockchain.ZkSyncEraTestnet,
            Blockchain.Moonbeam, Blockchain.MoonbeamTestnet,
            Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet,
            Blockchain.Moonriver, Blockchain.MoonriverTestnet,
            Blockchain.Taraxa, Blockchain.TaraxaTestnet,
            Blockchain.Cyber, Blockchain.CyberTestnet,
            Blockchain.Blast, Blockchain.BlastTestnet,
            Blockchain.Core, Blockchain.CoreTestnet, // base fee is zero
            Blockchain.EnergyWebChain, Blockchain.EnergyWebChainTestnet,
            Blockchain.Xodex,
            Blockchain.Chiliz, Blockchain.ChilizTestnet,
            Blockchain.VanarChain, Blockchain.VanarChainTestnet,
            Blockchain.Bitrock, Blockchain.BitrockTestnet,
            Blockchain.ZkLinkNova, Blockchain.ZkLinkNovaTestnet,
            Blockchain.Quai, Blockchain.QuaiTestnet,
            Blockchain.Adi, Blockchain.AdiTestnet,
            -> false
            else -> error("Don't forget about evm here")
        }
    }

val Blockchain.isGaslessTxSupported: Boolean
    get() {
        if (!isEvm()) return false

        return when (this) {
            Blockchain.Ethereum,
            Blockchain.BSC,
            Blockchain.Base,
            Blockchain.Polygon,
            Blockchain.Arbitrum,
            Blockchain.XDC,
            Blockchain.Optimism,
            -> true
            else -> false
        }
    }