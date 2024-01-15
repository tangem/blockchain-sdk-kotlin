package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.hedera.HederaTransactionBuilder
import com.tangem.blockchain.blockchains.hedera.HederaWalletManager
import com.tangem.blockchain.blockchains.hedera.network.HederaMirrorRestProvider
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.*

internal object HederaWalletManagerAssembly : WalletManagerAssembly<HederaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): HederaWalletManager {
        val mirrorNodeBaseUrl =
            if (input.wallet.blockchain.isTestnet()) API_HEDERA_MIRROR_TESTNET else API_HEDERA_MIRROR

        return HederaWalletManager(
            wallet = input.wallet,
            transactionBuilder = HederaTransactionBuilder(
                curve = input.curve,
                wallet = input.wallet,
            ),
            networkProvider = HederaMirrorRestProvider(mirrorNodeBaseUrl)
        )
    }
}
