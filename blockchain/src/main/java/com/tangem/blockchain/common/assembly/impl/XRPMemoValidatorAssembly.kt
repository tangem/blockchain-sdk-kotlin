package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.xrp.XRPProvidersBuilder
import com.tangem.blockchain.blockchains.xrp.XrpMemoValidator
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.common.assembly.MemoValidatorAssembly
import com.tangem.blockchain.common.assembly.MemoValidatorAssemblyInput
import com.tangem.blockchain.common.memo.MemoValidator

internal class XRPMemoValidatorAssembly : MemoValidatorAssembly() {

    override fun make(input: MemoValidatorAssemblyInput): MemoValidator {
        val networkService = XrpNetworkService(
            providers = XRPProvidersBuilder(input.providerTypes, input.config).build(input.blockchain),
            blockchain = input.blockchain,
        )
        return XrpMemoValidator(networkService)
    }
}