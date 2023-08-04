package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.solana.SolanaRpcClientBuilder
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object SolanaWalletManagerAssembly : WalletManagerAssembly<SolanaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): SolanaWalletManager {
        with(input.wallet) {
            val clients = SolanaRpcClientBuilder()
                .build(isTestnet = blockchain.isTestnet(), config = input.config)

            return SolanaWalletManager(wallet = this, providers = clients)
        }
    }

}