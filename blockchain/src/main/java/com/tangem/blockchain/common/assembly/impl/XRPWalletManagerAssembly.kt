package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_XRP_LEDGER_FOUNDATION

internal object XRPWalletManagerAssembly : WalletManagerAssembly<XrpWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): XrpWalletManager {
        val networkService = XrpNetworkService(
            providers = buildList {
                add(RippledNetworkProvider(baseUrl = API_XRP_LEDGER_FOUNDATION))
                input.config.nowNodeCredentials?.apiKey?.letNotBlank { apiKey ->
                    add(
                        RippledNetworkProvider(
                            baseUrl = "https://xrp.nownodes.io/",
                            apiKeyHeader = NowNodeCredentials.headerApiKey to apiKey,
                        ),
                    )
                }
                input.config.getBlockCredentials?.xrp?.jsonRpc.letNotBlank { jsonRpcToken ->
                    add(
                        RippledNetworkProvider(
                            baseUrl = "https://go.getblock.io/$jsonRpcToken/",
                        ),
                    )
                }
            },
        )

        return XrpWalletManager(
            wallet = input.wallet,
            transactionBuilder = XrpTransactionBuilder(networkService, input.wallet.publicKey.blockchainKey),
            networkProvider = networkService,
        )
    }
}
