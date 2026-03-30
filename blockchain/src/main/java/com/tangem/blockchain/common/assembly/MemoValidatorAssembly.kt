package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.memo.MemoValidator
import com.tangem.blockchain.common.network.providers.ProviderType

internal abstract class MemoValidatorAssembly {

    internal abstract fun make(input: MemoValidatorAssemblyInput): MemoValidator
}

internal class MemoValidatorAssemblyInput(
    val blockchain: Blockchain,
    val config: BlockchainSdkConfig,
    val providerTypes: List<ProviderType>,
)