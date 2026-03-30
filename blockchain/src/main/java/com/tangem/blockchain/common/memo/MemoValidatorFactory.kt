package com.tangem.blockchain.common.memo

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.assembly.MemoValidatorAssembly
import com.tangem.blockchain.common.assembly.MemoValidatorAssemblyInput
import com.tangem.blockchain.common.assembly.impl.StellarMemoValidatorAssembly
import com.tangem.blockchain.common.assembly.impl.XRPMemoValidatorAssembly
import com.tangem.blockchain.common.network.providers.ProviderType

class MemoValidatorFactory(
    private val config: BlockchainSdkConfig = BlockchainSdkConfig(),
    private val blockchainProviderTypes: Map<Blockchain, List<ProviderType>>,
) {

    fun create(blockchain: Blockchain): MemoValidator {
        val assembly = getAssembly(blockchain) ?: return DefaultMemoValidator
        val providerTypes = blockchainProviderTypes[blockchain].orEmpty()
        return assembly.make(
            input = MemoValidatorAssemblyInput(
                blockchain = blockchain,
                config = config,
                providerTypes = providerTypes,
            ),
        )
    }

    private fun getAssembly(blockchain: Blockchain): MemoValidatorAssembly? {
        return when (blockchain) {
            Blockchain.XRP -> XRPMemoValidatorAssembly()
            Blockchain.Stellar, Blockchain.StellarTestnet -> StellarMemoValidatorAssembly()
            else -> null
        }
    }
}