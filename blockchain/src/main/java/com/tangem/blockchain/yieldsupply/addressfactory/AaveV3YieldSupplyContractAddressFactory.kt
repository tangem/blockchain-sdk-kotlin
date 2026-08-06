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
        Blockchain.Polygon -> "0xb49CF4ba3c821560b5A4E6474D28f547368346CF"
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
        Blockchain.Polygon -> "0xB04aFaA060097C4a2c9e45FE611BB5db682C9aD6"
        Blockchain.EthereumTestnet -> "0x9A4b70A216C1A84d72a490f8cD3014Fdb538d249"
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    fun getSwapExecutionRegistryAddress(): String? = when (blockchain) {
        Blockchain.Ethereum -> "0xF9d772c558743749C91B1A76aA708ae61BC4716c"
        Blockchain.Avalanche -> "0x353CAee864B880619449Dd52EfBd37293eA222e5"
        Blockchain.Arbitrum -> "0x66084220E3dFdd1D8C8F1F868C103F9418DEce7c"
        Blockchain.Optimism -> "0x353CAee864B880619449Dd52EfBd37293eA222e5"
        Blockchain.Base -> "0x5b67AC3d1865F09712438D9522Bf8CCAB66a7b0D"
        Blockchain.BSC -> "0xc3E6FB1536510a4bDa260c25938E34EBc2Db9e33"
        Blockchain.Polygon -> "0x7125Ff05BB118Deb2d8DAA2e29beEfa02c20671F"
        else -> null
    }

    fun getLatestImplementationAddress(): String? = when (blockchain) {
        Blockchain.Ethereum -> "0xa6a6afa45D22aE7a55abC5cbBF426Fc8Dd45b846"
        Blockchain.Avalanche -> "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3"
        Blockchain.Arbitrum -> "0xDC8123e7E28D8cC12c3420CF8c8D6eceD9db4c71"
        Blockchain.Optimism -> "0xe1d0BF13C427C4B2e25Df0CA29E1Faa2d10458f3"
        Blockchain.Base -> "0x66cC410eC0Dd4013b7dA0a003404F6c503109093"
        Blockchain.BSC -> "0x6bBB8DDB265A6bae01422fF815a77e72D71F4e17"
        Blockchain.Polygon -> "0x66084220E3dFdd1D8C8F1F868C103F9418DEce7c"
        else -> null
    }
}