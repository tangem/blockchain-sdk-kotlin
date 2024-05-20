package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.common.card.EllipticCurve

/**
 * Data needed for WalletManager creation
 */
internal class WalletManagerAssemblyInput(
    val wallet: Wallet,
    val config: BlockchainSdkConfig,
    val curve: EllipticCurve,
    val providerTypes: List<ProviderType>,
)