package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object DucatusWalletManagerAssembly : WalletManagerAssembly<DucatusWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DucatusWalletManager {
        with(input.wallet) {
            return DucatusWalletManager(
                this,
                BitcoinTransactionBuilder(publicKey.blockchainKey, blockchain),
                DucatusNetworkService()
            )
        }
    }

}