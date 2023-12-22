package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ton.TonJsonRpcClientBuilder
import com.tangem.blockchain.blockchains.ton.TonWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object TonWalletManagerAssembly : WalletManagerAssembly<TonWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TonWalletManager {
        return TonWalletManager(
            wallet = input.wallet,
            networkProviders = TonJsonRpcClientBuilder()
                .build(
                    isTestNet = input.wallet.blockchain.isTestnet(),
                    blockchainSdkConfig = input.config,
                ),
        )
    }
}
