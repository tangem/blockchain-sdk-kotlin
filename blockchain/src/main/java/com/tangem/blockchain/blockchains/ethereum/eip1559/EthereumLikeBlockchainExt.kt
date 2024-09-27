package com.tangem.blockchain.blockchains.ethereum.eip1559

import com.tangem.blockchain.common.Blockchain

/** Returns true if the blockchain supports EIP1559 */
internal val Blockchain.isSupportEIP1559: Boolean
    get() {
        if (!isEvm()) return false

        return when (this) {
            Blockchain.Ethereum,
            Blockchain.BSC,
            Blockchain.Polygon,
            Blockchain.Avalanche,
            Blockchain.Fantom,
            Blockchain.Arbitrum,
            Blockchain.Gnosis,
            Blockchain.Optimism,
            Blockchain.Cronos,
            Blockchain.Decimal,
            Blockchain.Areon,
            Blockchain.Playa3ull,
            Blockchain.PulseChain,
            Blockchain.Manta,
            Blockchain.Mantle,
            Blockchain.Flare,
            Blockchain.Base,
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
            Blockchain.Cyber,
            Blockchain.Blast,
            -> false
            else -> error("Don't forget about evm here")
        }
    }