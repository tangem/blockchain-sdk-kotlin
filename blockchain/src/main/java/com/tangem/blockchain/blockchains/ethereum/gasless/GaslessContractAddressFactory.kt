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
            Blockchain.Ethereum -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
            Blockchain.BSC -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
            Blockchain.Base -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
            Blockchain.Polygon -> "0x0Ac67D0473f6fCbb28cf1cC06B172b1c8459Efc4"
            Blockchain.Arbitrum -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
            Blockchain.XDC -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
            Blockchain.Optimism -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
            else -> error("Gasless contract address not defined for blockchain: $blockchain")
        }
    }
}