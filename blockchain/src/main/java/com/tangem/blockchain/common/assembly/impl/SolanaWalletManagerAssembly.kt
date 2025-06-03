package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.solana.SolanaProvidersBuilder
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.nft.NFTProviderFactory

internal object SolanaWalletManagerAssembly : WalletManagerAssembly<SolanaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): SolanaWalletManager {
        return with(input.wallet) {
            SolanaWalletManager(
                wallet = this,
                providers = SolanaProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                nftProvider = NFTProviderFactory.createNFTProvider(blockchain, input.config),
            )
        }
    }
}