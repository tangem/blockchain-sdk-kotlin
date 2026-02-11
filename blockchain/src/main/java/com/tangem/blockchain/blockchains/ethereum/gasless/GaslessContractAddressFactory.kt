package com.tangem.blockchain.blockchains.ethereum.gasless

import com.tangem.blockchain.common.Blockchain

/**
 *  Factory to provide contract addresses gasless transactions based on the blockchain.
 */
internal class GaslessContractAddressFactory(
    private val blockchain: Blockchain,
) {

    // TODO(Add correct addresses when they will be available)
    fun getGaslessExecutorContractAddress(): String {
        return when (blockchain) {
            Blockchain.Ethereum -> "0xe3014E9AB2739aDeF234B3829C79128746160178"
            Blockchain.BSC -> "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3"
            Blockchain.Base -> "0x61dD8620410a2372CbE4946f9148671F38F93fC7"
            Blockchain.Polygon -> "0x2C2397c7605dc6d5493518260BDdeebE743B3faD"
            Blockchain.Arbitrum -> "0x20e7016ff14Dd10f04028fE52aBBca34F44b6965"
            else -> error("Gasless contract address not defined for blockchain: $blockchain")
        }
    }
}