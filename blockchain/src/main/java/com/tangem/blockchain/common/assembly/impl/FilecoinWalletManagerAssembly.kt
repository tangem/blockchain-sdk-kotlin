package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.filecoin.FilecoinProvidersBuilder
import com.tangem.blockchain.blockchains.filecoin.FilecoinWalletManager
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

/**
 * Filecoin [WalletManagerAssembly]
 *
 * @author Andrew Khokhlov on 24/07/2024
 */
internal object FilecoinWalletManagerAssembly : WalletManagerAssembly<FilecoinWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): FilecoinWalletManager {
        return with(input.wallet) {
            FilecoinWalletManager(
                wallet = this,
                networkService = FilecoinNetworkService(
                    providers = FilecoinProvidersBuilder(
                        providerTypes = input.providerTypes,
                        config = input.config,
                    ).build(blockchain),
                ),
            )
        }
    }
}
