package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.blockchains.ton.TonJsonRpcClientBuilder
import com.tangem.blockchain.blockchains.ton.TonWalletManager
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.blockchains.xrp.XrpWalletManager
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkService
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_XRP_LEDGER_FOUNDATION

object TonWalletManagerAssembly : WalletManagerAssembly<TonWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TonWalletManager {
        return TonWalletManager(
            wallet = input.wallet,
            networkProviders = TonJsonRpcClientBuilder()
                .build(
                    isTestNet = input.wallet.blockchain.isTestnet(),
                    blockchainSdkConfig = input.config
                ),
        )
    }
}