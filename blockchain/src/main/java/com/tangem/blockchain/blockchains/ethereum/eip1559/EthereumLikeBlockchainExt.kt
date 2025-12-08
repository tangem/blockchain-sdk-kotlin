package com.tangem.blockchain.blockchains.ethereum.eip1559

import com.tangem.blockchain.common.Blockchain

/** Returns true if the blockchain supports EIP1559 */
val Blockchain.isSupportEIP1559: Boolean
    get() {
        if (!isEvm()) return false

        return when (this) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet,
            Blockchain.BSC,
            Blockchain.Polygon,
            Blockchain.Avalanche,
            Blockchain.Fantom,
            Blockchain.Arbitrum,
            Blockchain.Gnosis,
            // TODO: [REDACTED_JIRA]
            // Blockchain.Optimism,
            // Blockchain.Manta,
            // Blockchain.Base,
            Blockchain.Cronos,
            Blockchain.Decimal,
            Blockchain.Areon,
            Blockchain.Playa3ull,
            Blockchain.PulseChain,
            Blockchain.Flare,
            Blockchain.Canxium,
            Blockchain.OdysseyChain,
            Blockchain.Sonic,
            Blockchain.ApeChain,
            -> true
            Blockchain.EthereumClassic, // eth_feeHistory all zeroes
            Blockchain.EthereumPow, // eth_feeHistory with zeros
            Blockchain.Dischain, // eth_feeHistory with zeros
            Blockchain.Kava, // eth_feeHistory zero or null
            Blockchain.OctaSpace, // eth_feeHistory all zeroes
            Blockchain.Shibarium, // wrong base fee in eth_feeHistory. wei instead of gwei
            Blockchain.RSK,
            Blockchain.Telos,
            Blockchain.XDC,
            Blockchain.Aurora,
            Blockchain.ZkSyncEra,
            Blockchain.Moonbeam,
            Blockchain.PolygonZkEVM,
            Blockchain.Moonriver,
            Blockchain.Taraxa,
            Blockchain.Optimism,
            Blockchain.Manta,
            Blockchain.Base,
            Blockchain.Cyber,
            Blockchain.Blast,
            Blockchain.Core, // base fee is zero
            Blockchain.EnergyWebChain,
            Blockchain.Mantle,
            Blockchain.Xodex,
            Blockchain.Chiliz,
            Blockchain.VanarChain,
            Blockchain.Bitrock,
            Blockchain.Scroll,
            Blockchain.ZkLinkNova,
            Blockchain.Hyperliquid,
            Blockchain.Quai,
            Blockchain.Ink,
            Blockchain.InkTestnet,
            Blockchain.Lisk,
            Blockchain.LiskTestnet,
            Blockchain.Mode,
            Blockchain.ModeTestnet,
            Blockchain.SwellChain,
            Blockchain.SwellChainTestnet,
            Blockchain.Superseed,
            Blockchain.SuperseedTestnet,
            Blockchain.Bob,
            Blockchain.BobTestnet,
            Blockchain.Soneium,
            Blockchain.SoneiumTestnet,
            Blockchain.Unichain,
            Blockchain.UnichainTestnet,
            Blockchain.MetalL2,
            Blockchain.MetalL2Testnet,
            Blockchain.Celo,
            Blockchain.CeloTestnet,
            Blockchain.Fraxtal,
            Blockchain.FraxtalTestnet,
            -> false
            else -> error("Don't forget about evm here")
        }
    }