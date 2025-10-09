package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider

@OptIn(ExperimentalStdlibApi::class)
internal open class EthereumNetworkService(
    multiJsonRpcProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
    blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    blockchairEthNetworkProvider: BlockchairEthNetworkProvider? = null,
) : EthereumLikeNetworkService(
    multiJsonRpcProvider = multiJsonRpcProvider,
    yieldSupplyProvider = yieldSupplyProvider,
    blockcypherNetworkProvider = blockcypherNetworkProvider,
    blockchairEthNetworkProvider = blockchairEthNetworkProvider,
) {

    override fun getDecimals(): Int = Blockchain.Ethereum.decimals()
}