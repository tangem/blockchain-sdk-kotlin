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
        return when (blockchain) {
            Blockchain.Ethereum -> "0xe3014E9AB2739aDeF234B3829C79128746160178"
            Blockchain.BSC -> "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3"
            Blockchain.Base -> "0x61dD8620410a2372CbE4946f9148671F38F93fC7"
            Blockchain.Polygon -> if (isV2) {
                "0x5c5eB829353bdb38456B54480aB436cAE421B75C"
            } else {
                "0x2C2397c7605dc6d5493518260BDdeebE743B3faD"
            }
            Blockchain.Arbitrum -> "0x20e7016ff14Dd10f04028fE52aBBca34F44b6965"
            else -> error("Gasless contract address not defined for blockchain: $blockchain")
        }
    }
}