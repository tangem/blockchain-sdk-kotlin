package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.hedera.HederaTransactionBuilder
import com.tangem.blockchain.blockchains.hedera.HederaWalletManager
import com.tangem.blockchain.blockchains.hedera.network.HederaMirrorRestProvider
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkProvider
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_HEDERA_ARKHIA_MIRROR
import com.tangem.blockchain.network.API_HEDERA_ARKHIA_MIRROR_TESTNET
import com.tangem.blockchain.network.API_HEDERA_MIRROR
import com.tangem.blockchain.network.API_HEDERA_MIRROR_TESTNET

internal class HederaWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
    private val accountCreator: AccountCreator
) : WalletManagerAssembly<HederaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): HederaWalletManager {
        val isTestnet = input.wallet.blockchain.isTestnet()
        val providers: List<HederaNetworkProvider> = buildList {
            add(HederaMirrorRestProvider(if (isTestnet) API_HEDERA_MIRROR_TESTNET else API_HEDERA_MIRROR))

            input.config.hederaArkhiaApiKey
                ?.letNotBlank {
                    add(
                        HederaMirrorRestProvider(
                            if (isTestnet) API_HEDERA_ARKHIA_MIRROR_TESTNET else API_HEDERA_ARKHIA_MIRROR,
                        ),
                    )
                }
        }

        return HederaWalletManager(
            wallet = input.wallet,
            transactionBuilder = HederaTransactionBuilder(
                curve = input.curve,
                wallet = input.wallet,
            ),
            networkProvider = HederaNetworkService(providers),
            dataStorage = dataStorage,
            accountCreator = accountCreator,
        )
    }
}