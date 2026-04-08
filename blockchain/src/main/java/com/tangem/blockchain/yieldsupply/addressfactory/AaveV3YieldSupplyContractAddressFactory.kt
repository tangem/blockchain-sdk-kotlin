package com.tangem.blockchain.yieldsupply.addressfactory

import com.tangem.blockchain.common.Blockchain

/**
 *  Factory to provide Aave contract addresses for Yield Module based on the blockchain.
 */
internal class AaveV3YieldSupplyContractAddressFactory(
    private val blockchain: Blockchain,
) {

    fun getFactoryAddress(): String = when (blockchain) {
        Blockchain.Ethereum -> "0xd8972a45616bEC62cB9687e38a99D763c0879B0d"
        Blockchain.Avalanche -> "0x7255BFf778243f58B53777878B931Df596e1816A"
        Blockchain.Arbitrum -> "0xb49CF4ba3c821560b5A4E6474D28f547368346CF"
        Blockchain.Optimism -> "0x7255BFf778243f58B53777878B931Df596e1816A"
        Blockchain.Base -> "0xC49B1438c8639AB48953e9091E5277D4C65003f0"
        Blockchain.BSC -> "0x7255BFf778243f58B53777878B931Df596e1816A"
        Blockchain.Polygon -> "0x1bE509C2fF23dF065E15A6d37b0eFe4c839c62fE"
        Blockchain.EthereumTestnet -> "0xF3b31452E8EE5B294D7172B69Bd02decF2255FCd"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getProcessorAddress(): String = when (blockchain) {
        Blockchain.Ethereum -> "0x4fF6178B58a51Cb74E50254ED1e9ebd4F28Eb2C0"
        Blockchain.Avalanche -> "0x1A5Dd8e4Feb0bb4E6765DAd78B83e8bA3fba2dAC"
        Blockchain.Arbitrum -> "0xF22E4A776cca26A003920538E174E3aeA8177d9f"
        Blockchain.Optimism -> "0x1A5Dd8e4Feb0bb4E6765DAd78B83e8bA3fba2dAC"
        Blockchain.Base -> "0x487C7bA76BB0611d20A97E89625Ca93c87Ed4AA1"
        Blockchain.BSC -> "0x1A5Dd8e4Feb0bb4E6765DAd78B83e8bA3fba2dAC"
        Blockchain.Polygon -> "0xD021F1D410aCB895aB110a0CbB740a33db209bDD"
        Blockchain.EthereumTestnet -> "0x9A4b70A216C1A84d72a490f8cD3014Fdb538d249"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getSwapExecutionRegistryAddress(): String? = when (blockchain) {
        Blockchain.Polygon -> "0x2F0C06606238abD3e45c2F8ED233A06FDD7F454d"
        else -> null
    }

    // TODO: Replace with actual latest implementation addresses per chain when available
    @Suppress("FunctionOnlyReturningConstant")
    fun getLatestImplementationAddress(): String? = null
}