package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProvider

class BitcoinCashNetworkService(
        blockchairApiKey: String? = null,
        blockchairAuthorizationToken: String? = null
) :
        BlockchairNetworkProvider(Blockchain.BitcoinCash, blockchairApiKey)