package com.tangem.blockchain.blockchains.quai

import com.tangem.blockchain.blockchains.ethereum.network.EthereumLikeNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.blockchair.BlockchairEthNetworkProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider

/**
 * Network service for Quai Network
 * Uses Quai-specific RPC methods with quai_ prefix
 */
internal class QuaiNetworkService(
    multiJsonRpcProvider: MultiNetworkProvider<QuaiJsonRpcProvider>,
    yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
    blockcypherNetworkProvider: BlockcypherNetworkProvider? = null,
    blockchairEthNetworkProvider: BlockchairEthNetworkProvider? = null,
) : EthereumLikeNetworkService(
    multiJsonRpcProvider = multiJsonRpcProvider,
    yieldSupplyProvider = yieldSupplyProvider,
    blockcypherNetworkProvider = blockcypherNetworkProvider,
    blockchairEthNetworkProvider = blockchairEthNetworkProvider,
) {

    override fun getDecimals(): Int = Blockchain.Quai.decimals()
}