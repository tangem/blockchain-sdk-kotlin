package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.stellar.StellarMemoValidator
import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.MemoValidatorAssembly
import com.tangem.blockchain.common.assembly.MemoValidatorAssemblyInput
import com.tangem.blockchain.common.memo.MemoValidator

internal class StellarMemoValidatorAssembly : MemoValidatorAssembly() {

    override fun make(input: MemoValidatorAssemblyInput): MemoValidator {
        val isTestnet = input.blockchain == Blockchain.StellarTestnet
        val networkService = StellarNetworkService(
            isTestnet = isTestnet,
            providers = StellarProvidersBuilder(input.providerTypes, input.config).build(input.blockchain),
        )
        return StellarMemoValidator(networkService)
    }
}