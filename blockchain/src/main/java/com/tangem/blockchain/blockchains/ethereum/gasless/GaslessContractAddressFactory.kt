package com.tangem.blockchain.blockchains.ethereum.gasless

import com.tangem.blockchain.common.Blockchain

/**
 *  Factory to provide contract addresses gasless transactions based on the blockchain.
 */
internal class GaslessContractAddressFactory(
    private val blockchain: Blockchain,
) {
    /**
     * Returns the gasless executor address (EIP-7702 delegation target) for the current blockchain.
     *
     * @param isV2 selects the protocol generation
     */
    fun getGaslessExecutorContractAddress(isV2: Boolean): String {
        return if (isV2) getV2ExecutorAddress() else getV1ExecutorAddress()
    }

    private fun getV1ExecutorAddress(): String = when (blockchain) {
        Blockchain.Ethereum -> "0xe3014E9AB2739aDeF234B3829C79128746160178"
        Blockchain.BSC -> "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3"
        Blockchain.Polygon -> "0x2C2397c7605dc6d5493518260BDdeebE743B3faD"
        Blockchain.Arbitrum -> "0x20e7016ff14Dd10f04028fE52aBBca34F44b6965"
        Blockchain.Base -> "0x61dD8620410a2372CbE4946f9148671F38F93fC7"
        else -> error("Gasless contract address not defined for blockchain: $blockchain")
    }

    private fun getV2ExecutorAddress(): String = when (blockchain) {
        Blockchain.Ethereum -> "0xb94B392b61c16Ddb7118849D4970570C07F75dD1"
        Blockchain.BSC -> "0x96922f4b701F0138064bCcB1549B4B7B6b3447CC"
        Blockchain.Polygon -> "0x02a35743C4170A3685271708399311801a230cf0"
        Blockchain.Arbitrum -> "0x4E039670C679346f785D61a0e21aBe0330F1b776"
        Blockchain.Base -> "0xA787dd893e772c42cCe545A2560D53AcdDe251A6"
        else -> error("Gasless contract address not defined for blockchain: $blockchain")
    }
}